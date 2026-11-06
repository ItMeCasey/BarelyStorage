package com.itmecasey.barelystorage.menu;

import com.itmecasey.barelystorage.CraftPlan;
import com.itmecasey.barelystorage.CraftScheduler;
import com.itmecasey.barelystorage.Keys;
import com.itmecasey.barelystorage.ModContent;
import com.itmecasey.barelystorage.StorageNetwork;
import com.itmecasey.barelystorage.block.RackBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** The Crafting Queue Terminal: the running and waiting crafting jobs with their progress and any halt error. Button id n cancels job n. */
public class QueueMenu extends StorageMenu {
    public static final int JOBS = 6;
    /** Per job: state, steps done, steps total. */
    public static final int PER_JOB = 3;
    public static final int D_ERROR = JOBS * PER_JOB;
    public static final int D_NEED_A = D_ERROR + 1;
    public static final int D_HAVE_A = D_ERROR + 2;
    public static final int D_ONLINE = D_ERROR + 3;
    public static final int D_COUNT = D_ERROR + 4;

    @Nullable
    private final Level level;
    @Nullable
    private final BlockPos pos;
    private final Container jobStacks = new SimpleContainer(JOBS);
    private final ContainerData data = new SimpleContainerData(D_COUNT);
    private long lastRefresh = -100;
    private final int[] jobIds = new int[JOBS];

    public QueueMenu(int id, Inventory inventory) {
        this(id, inventory, null, null);
    }

    public QueueMenu(int id, Inventory inventory, @Nullable Level level, @Nullable BlockPos pos) {
        super(ModContent.QUEUE_MENU, id);
        this.level = level;
        this.pos = pos;
        for (int i = 0; i < JOBS; i++) {
            addSlot(new Slot(jobStacks, i, 8, 30 + i * 20) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }
            });
        }
        playerSlots(inventory, 8, 30 + JOBS * 20 + 24);
        addDataSlots(data);
    }

    public int data(int i) {
        return data.get(i);
    }

    private void refresh() {
        if (level == null || pos == null) {
            return;
        }
        lastRefresh = level.getGameTime();
        StorageNetwork net = StorageNetwork.find(level, pos);
        RackBlockEntity lead = net.leader();
        data.set(D_ONLINE, lead == null ? 0 : 1);
        for (int i = 0; i < D_COUNT - 1; i++) {
            if (i < JOBS * PER_JOB || i == D_ERROR || i == D_NEED_A || i == D_HAVE_A) {
                data.set(i, 0);
            }
        }
        for (int i = 0; i < JOBS; i++) {
            jobStacks.setItem(i, ItemStack.EMPTY);
            jobIds[i] = -1;
        }
        if (lead == null) {
            return;
        }
        List<CraftScheduler.Job> jobs = lead.scheduler().jobs();
        for (int i = 0; i < Math.min(JOBS, jobs.size()); i++) {
            CraftScheduler.Job j = jobs.get(i);
            jobIds[i] = j.id;
            jobStacks.setItem(i, Keys.stack(j.target, (int) Math.max(1, Math.min(Integer.MAX_VALUE, j.amount)), net.registries()));
            data.set(i * PER_JOB, j.state == CraftScheduler.State.RUNNING ? 1 : j.state == CraftScheduler.State.QUEUED ? 2 : j.state == CraftScheduler.State.HALTED ? 3 : 0);
            data.set(i * PER_JOB + 1, j.stepsDone());
            data.set(i * PER_JOB + 2, j.steps.size());
            if (i == 0 && j.state == CraftScheduler.State.HALTED) {
                CraftPlan.Error e = j.plan.error;
                data.set(D_ERROR, e == CraftPlan.Error.NO_CORE ? 1 : e == CraftPlan.Error.THREAD_EXHAUSTION ? 2 : e == CraftPlan.Error.OUT_OF_MEMORY ? 3 : 4);
                data.set(D_NEED_A, e == CraftPlan.Error.OUT_OF_MEMORY ? j.plan.ram : j.plan.depth);
                data.set(D_HAVE_A, e == CraftPlan.Error.OUT_OF_MEMORY ? net.hardware().ram() : Math.min(99, net.hardware().maxDepth()));
            }
        }
    }

    @Override
    public void broadcastChanges() {
        if (level != null && !level.isClientSide() && level.getGameTime() - lastRefresh >= 5) {
            refresh();
        }
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (level == null || level.isClientSide() || pos == null || id < 0 || id >= JOBS || jobIds[id] < 0) {
            return false;
        }
        RackBlockEntity lead = StorageNetwork.find(level, pos).leader();
        if (lead != null) {
            lead.scheduler().cancel(jobIds[id]);
            lead.setChanged();
        }
        lastRefresh = -100;
        return true;
    }

    public int playerY() {
        return 30 + JOBS * 20 + 24;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return pos == null || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
}
