package github.kasuminova.novaeng.mixin.minecraft.forge;

import github.kasuminova.novaeng.common.performance.SpawnCandidateList;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.world.WorldEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Mixin(value = WorldEvent.PotentialSpawns.class, remap = false)
public abstract class MixinPotentialSpawnsAllocation {
    // Both field writes below discard this placeholder; constructor redirects cannot return null.
    @Unique
    private static final ArrayList<Biome.SpawnListEntry> nova$unusedConstructorList = new ArrayList<>(0);
    @Shadow
    @Final
    @Mutable
    private List<Biome.SpawnListEntry> list;

    @Redirect(method = "<init>", at = @At(value = "NEW", target = "(Ljava/util/Collection;)Ljava/util/ArrayList;"), require = 1)
    private ArrayList<Biome.SpawnListEntry> nova$deferCopy(final Collection<Biome.SpawnListEntry> c) {
        return nova$unusedConstructorList;
    }

    @Redirect(method = "<init>", at = @At(value = "NEW", target = "()Ljava/util/ArrayList;"), require = 1)
    private ArrayList<Biome.SpawnListEntry> nova$deferEmptyList() {
        return nova$unusedConstructorList;
    }

    @Redirect(method = "<init>", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
        target = "Lnet/minecraftforge/event/world/WorldEvent$PotentialSpawns;list:Ljava/util/List;"), require = 2)
    private void nova$ownCandidateList(final WorldEvent.PotentialSpawns event, final List<Biome.SpawnListEntry> ignored,
                                       final World world, final EnumCreatureType type, final BlockPos pos,
                                       final List<Biome.SpawnListEntry> oldList) {
        list = SpawnCandidateList.copyOf(oldList);
    }
}
