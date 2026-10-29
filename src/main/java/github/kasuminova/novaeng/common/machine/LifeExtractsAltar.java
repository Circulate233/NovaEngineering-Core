package github.kasuminova.novaeng.common.machine;

import crafttweaker.CraftTweakerAPI;
import github.kasuminova.mmce.common.event.client.ControllerGUIRenderEvent;
import github.kasuminova.mmce.common.event.machine.MachineStructureUpdateEvent;
import github.kasuminova.mmce.common.event.recipe.RecipeEvent;
import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.common.util.Functions;
import github.kasuminova.novaeng.common.util.RecipePrimerEx;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipeBuilder;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public final class LifeExtractsAltar implements MachineSpecial {
    public static final LifeExtractsAltar INSTANCE = new LifeExtractsAltar();
    public static final int SINGLE_VALUE = 5000;
    public static final String MACHINEID = "life_extracts_altar";
    public static final ResourceLocation REGISTRY_NAME = new ResourceLocation(ModularMachinery.MODID, MACHINEID);

    private LifeExtractsAltar() {
    }

    @Override
    public ResourceLocation getRegistryName() {
        return REGISTRY_NAME;
    }

    @Override
    public void preInit(final DynamicMachine machine) {
        MachineSpecial.super.init(machine);
        machine.addMachineEventHandler(MachineStructureUpdateEvent.class,
            event -> event.getController().setWorkMode(TileMultiblockMachineController.WorkMode.SEMI_SYNC));
        final var recipe = RecipeBuilder.newBuilder("knzj", MACHINEID, 20)
                                        .addItemInput(CraftTweakerAPI.itemUtils.getItem("deepmoblearningbm:digital_agonizer", 0)).setChance(0)
                                        .addItemInput(CraftTweakerAPI.itemUtils.getItem("contenttweaker:zbk", 0)).setChance(0);
        RecipePrimerEx.setLore(recipe, "novaeng.life_extracts_altar.recipe.0")
                      .setNBTChecker((controller, item) -> {
                          final var input = ((ItemStack) item.getInternal()).getTagCompound();
                          if (input == null || !input.hasKey("binding")) {
                              return false;
                          }
                          final var data = controller.getController().getCustomDataTag();
                          data.setTag("x", input.getTag("x"));
                          data.setTag("y", input.getTag("y"));
                          data.setTag("z", input.getTag("z"));
                          return true;
                      }).addPostCheckHandler(event -> {
                          final var controller = this.getAltarCtrl(event);
                          if (controller == null) {
                              event.setFailed("novaeng.life_extracts_altar.failed.0");
                              return;
                          }
                          if (!controller.getWorld().isRemote) {
                              final var altar = MMAltar.getAltar(controller);
                              if (altar == null) {
                                  event.setFailed("novaeng.life_extracts_altar.failed.1");
                                  return;
                              }
                              if (!MMAltar.checkRitualWellOfSuffering(controller.getCustomDataTag(), controller)) {
                                  event.setFailed("novaeng.life_extracts_altar.failed.2");
                              }
                          }
                      }).addStartHandler(event -> event.getController().getCustomDataTag().setBoolean("mode", true))
                      .addPreTickHandler(event -> {
                          if (event.getActiveRecipe().getTick() != 0) {
                              return;
                          }
                          final var controller = this.getAltarCtrl(event);
                          if (controller == null) {
                              event.setFailed(true, "novaeng.life_extracts_altar.failed.0");
                              return;
                          }
                          if (!controller.getWorld().isRemote) {
                              final var altar = MMAltar.getAltar(controller);
                              if (altar == null) {
                                  event.setFailed(true, "novaeng.life_extracts_altar.failed.1");
                                  return;
                              }
                              if (!MMAltar.checkRitualWellOfSuffering(controller.getCustomDataTag(), controller)) {
                                  event.setFailed(true, "novaeng.life_extracts_altar.failed.2");
                              }
                          }
                      }).addFinishHandler(event -> {
                          final var controller = this.getAltarCtrl(event);
                          if (controller != null) {
                              MMAltar.addBlood(MMAltar.getAltar(controller), SINGLE_VALUE + SINGLE_VALUE / 10 * controller.getCustomDataTag().getShort("sacrifice"));
                          }
                      }).addRecipeTooltip("novaeng.life_extracts_altar.recipe.1", "novaeng.life_extracts_altar.recipe.2",
                          "novaeng.life_extracts_altar.recipe.3", Functions.getText("novaeng.life_extracts_altar.recipe.4", SINGLE_VALUE))
                      .setParallelized(false).build();
        if (NovaEngineeringCore.proxy.isClient()) {
            this.clientInit(machine);
        }
    }

    private TileMultiblockMachineController getAltarCtrl(final RecipeEvent event) {
        final var controller = event.getController();
        final var data = controller.getCustomDataTag();
        final var world = controller.getWorld();
        final var position = BlockPos.PooledMutableBlockPos.retain(data.getInteger("x"), data.getInteger("y"), data.getInteger("z"));
        try {
            if (!world.isBlockLoaded(position)) {
                return null;
            }
            return world.getTileEntity(position) instanceof TileMultiblockMachineController altar ? altar : null;
        } finally {
            position.release();
        }
    }

    @SideOnly(Side.CLIENT)
    private void clientInit(final DynamicMachine machine) {
        machine.addMachineEventHandler(ControllerGUIRenderEvent.class, event -> event.setExtraInfo(
            Functions.getText("gui.life_extracts_altar.tooltip.name"),
            Functions.getText("gui.life_extracts_altar.tooltip." + event.getController().getCustomDataTag().getBoolean("mode"))));
    }
}
