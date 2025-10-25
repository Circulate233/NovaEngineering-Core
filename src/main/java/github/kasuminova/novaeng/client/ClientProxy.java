package github.kasuminova.novaeng.client;

import github.kasuminova.mmce.client.renderer.MachineControllerRenderer;
import github.kasuminova.novaeng.NovaEngineeringCore;
import github.kasuminova.novaeng.client.book.BookTransformerAppendModifiers;
import github.kasuminova.novaeng.client.gui.GuiECalculatorController;
import github.kasuminova.novaeng.client.gui.GuiEFabricatorController;
import github.kasuminova.novaeng.client.gui.GuiEFabricatorPatternBus;
import github.kasuminova.novaeng.client.gui.GuiEFabricatorPatternSearch;
import github.kasuminova.novaeng.client.gui.GuiEStorageController;
import github.kasuminova.novaeng.client.gui.GuiGeocentricDrill;
import github.kasuminova.novaeng.client.gui.GuiHyperNetTerminal;
import github.kasuminova.novaeng.client.gui.GuiModularServerAssembler;
import github.kasuminova.novaeng.client.gui.GuiSingularityCore;
import github.kasuminova.novaeng.client.handler.BlockAngelRendererHandler;
import github.kasuminova.novaeng.client.handler.ClientEventHandler;
import github.kasuminova.novaeng.client.handler.HyperNetClientEventHandler;
import github.kasuminova.novaeng.client.model.raw_ore.RawOreModelLoader;
import github.kasuminova.novaeng.client.util.ExJEI;
import github.kasuminova.novaeng.client.util.TitleUtils;
import github.kasuminova.novaeng.common.CommonProxy;
import github.kasuminova.novaeng.common.command.CommandPacketProfiler;
import github.kasuminova.novaeng.common.command.ExportResearchDataToJson;
import github.kasuminova.novaeng.NovaEngCoreConfig;
import github.kasuminova.novaeng.common.item.ItemRawOre;
import github.kasuminova.novaeng.common.registry.RegistryBlocks;
import github.kasuminova.novaeng.common.registry.RegistryItems;
import github.kasuminova.novaeng.common.tile.TileHyperNetTerminal;
import github.kasuminova.novaeng.common.tile.TileModularServerAssembler;
import github.kasuminova.novaeng.common.tile.ecotech.ecalculator.ECalculatorController;
import github.kasuminova.novaeng.common.tile.ecotech.efabricator.EFabricatorController;
import github.kasuminova.novaeng.common.tile.ecotech.efabricator.EFabricatorPatternBus;
import github.kasuminova.novaeng.common.tile.ecotech.estorage.EStorageController;
import github.kasuminova.novaeng.common.tile.machine.GeocentricDrillController;
import github.kasuminova.novaeng.common.tile.machine.SingularityCore;
import hellfirepvp.modularmachinery.common.base.Mods;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.resource.VanillaResourceType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.oredict.OreDictionary;
import slimeknights.tconstruct.library.book.TinkerBook;

import javax.annotation.Nullable;
import java.awt.Color;
import java.util.List;

import static github.kasuminova.novaeng.mixin.NovaEngCoreEarlyMixinLoader.checkJavaVersion;
import static github.kasuminova.novaeng.mixin.NovaEngCoreEarlyMixinLoader.isCleanroomLoader;

@SuppressWarnings("MethodMayBeStatic")
@Mod.EventBusSubscriber(Side.CLIENT)
public class ClientProxy extends CommonProxy {

    @Getter
    private static List<String> itemDisplayTooltip;
    private static final Object2IntMap<String> colorCache = new Object2IntOpenHashMap<>();
    public static List<Item> items = new ObjectArrayList<>();
    public static List<Block> blocks = new ObjectArrayList<>();

    static {
        colorCache.defaultReturnValue(-1);
    }

    public ClientProxy() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static int getColorForODFirst(String odName) {
        var color = colorCache.getInt(odName);
        if (color < 0) {
            var od = OreDictionary.getOres(odName);
            if (!od.isEmpty()) {
                var stack = od.get(0);
                var item = stack.getItem();
                color = getColorForItemStack(od.get(0)).getRGB();
            } else {
                color = Color.WHITE.getRGB();
            }
            colorCache.put(odName, color);
        }
        return color;
    }

    public static Color getColorForItemStack(ItemStack stack) {
        try {
            TextureAtlasSprite sprite;
            if (stack.getItem() instanceof ItemBlock) {
                Minecraft mc = Minecraft.getMinecraft();
                IBlockState state = ((ItemBlock) stack.getItem()).getBlock().getStateForPlacement(mc.world,
                        new BlockPos(0, 0, 0), EnumFacing.UP, 0, 0, 0, stack.getMetadata(), mc.player);
                List<BakedQuad> quads = mc.getBlockRendererDispatcher().getModelForState(state).getQuads(state, EnumFacing.NORTH, 0);
                if (quads.isEmpty()) return Color.WHITE;
                sprite = quads.get(0).getSprite();
            } else sprite = Minecraft.getMinecraft().getRenderItem().getItemModelWithOverrides(stack, null, null)
                    .getQuads(null, null, 0).get(0).getSprite();
            IntList colours = new IntArrayList();
            for (int[] rows : sprite.getFrameTextureData(0))
                for (int colour : rows) if ((colour & 0xFF) > 0) colours.add(colour);
            long r = 0, g = 0, b = 0;
            for (int colour : colours) {
                r += (colour >> 16) & 0xFF;
                g += (colour >> 8) & 0xFF;
                b += colour & 0xFF;
            }
            return new Color((int) r / colours.size(), (int) g / colours.size(), (int) b / colours.size(), 255);
        } catch (Exception e) {
            return Color.WHITE;
        }
    }

    public void setColor(String od, int color) {
        colorCache.put(od, color);
    }

    @Override
    public boolean isClient() {
        return true;
    }

    @Override
    public void construction() {
        super.construction();

        ConfigManager.sync(NovaEngineeringCore.MOD_ID, Config.Type.INSTANCE);

        if (NovaEngCoreConfig.javaCheck) {
            if (!isCleanroomLoader()) {
                checkJavaVersion();
            }
        }

        TitleUtils.setRandomTitle("*Construction*");
    }

    @Override
    public void preInit() {
        super.preInit();
        MinecraftForge.EVENT_BUS.register(HyperNetClientEventHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(ClientEventHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(BlockAngelRendererHandler.INSTANCE);

        itemDisplayTooltip = ObjectLists.singleton(I18n.format("key.novaeng.item_display.tooltip", "Ctrl + L"));

        if (Mods.GECKOLIB.isPresent()) {
            ClientRegistry.bindTileEntitySpecialRenderer(SingularityCore.class, MachineControllerRenderer.INSTANCE);
        }

        TitleUtils.setRandomTitle("*PreInit*");
    }

    @Override
    public void init() {
        super.init();

        TitleUtils.setRandomTitle("*Init*");

        if (Loader.isModLoaded("ic2") && Loader.isModLoaded("randomtweaker")) {
            ExJEI.jeiCreate();
        }
    }

    @Override
    public void postInit() {
        super.postInit();

        ClientCommandHandler.instance.registerCommand(ExportResearchDataToJson.INSTANCE);
        ClientCommandHandler.instance.registerCommand(CommandPacketProfiler.INSTANCE);

        TitleUtils.setRandomTitle("*PostInit*");

        Minecraft mc = Minecraft.getMinecraft();
        BlockColors blockColors = mc.getBlockColors();
        ItemColors itemColors = mc.getItemColors();

        itemColors.registerItemColorHandler((stack, i) -> {
            var item = stack.getItem();
            if (item instanceof ItemRawOre r) {
                return getColorForODFirst(r.getPartOD());
            }
            if (item instanceof ItemRawOre.BlockRawOre.ItemBLockRawOre r) {
                return getColorForODFirst(r.getPartOD());
            }
            return Color.WHITE.getRGB();
        }, items.toArray(new Item[0]));

        blockColors.registerBlockColorHandler((state, worldIn, pos, i) -> {
            if (state.getBlock() instanceof ItemRawOre.BlockRawOre blockRawOre) {
                return getColorForODFirst(blockRawOre.getPartOD());
            }
            return Color.WHITE.getRGB();
        }, blocks.toArray(new Block[0]));

        if (Loader.isModLoaded("ic2") && Loader.isModLoaded("randomtweaker")) {
            ExJEI.jeiRecipeRegister();
        }

        TinkerBook.INSTANCE.addTransformer(BookTransformerAppendModifiers.INSTANCE_FALSE);
        FMLClientHandler.instance().refreshResources(VanillaResourceType.TEXTURES, VanillaResourceType.MODELS);
    }

    @Override
    public void loadComplete() {
        super.loadComplete();

        TitleUtils.setRandomTitle();
    }

    @SubscribeEvent
    public void onModelRegister(ModelRegistryEvent event) {
        RegistryBlocks.registerBlockModels();
        RegistryItems.registerItemModels();
        ModelLoaderRegistry.registerLoader(new RawOreModelLoader());
    }

    @Nullable
    @Override
    public Object getClientGuiElement(final int ID, final EntityPlayer player, final World world, final int x, final int y, final int z) {
        GuiType type = GuiType.values()[MathHelper.clamp(ID, 0, GuiType.values().length - 1)];
        Class<? extends TileEntity> required = type.requiredTileEntity;
        TileEntity present = null;
        if (required != null) {
            TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
            if (te != null && required.isAssignableFrom(te.getClass())) {
                present = te;
            } else {
                return null;
            }
        }

        return switch (type) {
            case HYPERNET_TERMINAL -> new GuiHyperNetTerminal((TileHyperNetTerminal) present, player);
            case MODULAR_SERVER_ASSEMBLER ->
                    new GuiModularServerAssembler((TileModularServerAssembler) present, player);
            case ESTORAGE_CONTROLLER -> new GuiEStorageController((EStorageController) present, player);
            case SINGULARITY_CORE -> new GuiSingularityCore((SingularityCore) present, player);
            case EFABRICATOR_CONTROLLER -> new GuiEFabricatorController((EFabricatorController) present, player);
            case EFABRICATOR_PATTERN_SEARCH -> new GuiEFabricatorPatternSearch((EFabricatorController) present, player);
            case EFABRICATOR_PATTERN_BUS -> new GuiEFabricatorPatternBus((EFabricatorPatternBus) present, player);
            case GEOCENTRIC_DRILL_CONTROLLER -> new GuiGeocentricDrill((GeocentricDrillController) present, player);
            case ECALCULATOR_CONTROLLER -> new GuiECalculatorController((ECalculatorController) present, player);
        };
    }

}