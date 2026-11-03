package github.kasuminova.novaeng.common.performance;

import com.github.lunatrius.ingameinfo.tag.TagFormatting;
import com.github.lunatrius.ingameinfo.tag.TagMisc;
import com.github.lunatrius.ingameinfo.tag.TagMouseOver;
import com.github.lunatrius.ingameinfo.tag.TagNearbyPlayer;
import com.github.lunatrius.ingameinfo.tag.TagPlayerEquipment;
import com.github.lunatrius.ingameinfo.tag.TagPlayerGeneral;
import com.github.lunatrius.ingameinfo.tag.TagPlayerPosition;
import com.github.lunatrius.ingameinfo.tag.TagPlayerPotion;
import com.github.lunatrius.ingameinfo.tag.TagRiding;
import com.github.lunatrius.ingameinfo.tag.TagTime;
import com.github.lunatrius.ingameinfo.tag.TagWorld;
import com.github.lunatrius.ingameinfo.tag.registry.TagRegistry;

/**
 * Original built-in registration names/order with deferred value implementations.
 */
public final class LazyBuiltinTags {
    private LazyBuiltinTags() {
    }

    public static void registerFormatting() {
        TagRegistry.INSTANCE.register((new DeferredTag("formatting", -1, -1, TagFormatting.Black::new)).setName("black"));
        TagRegistry.INSTANCE.register((new DeferredTag("formatting", -1, -1, TagFormatting.DarkBlue::new)).setName("darkblue").setAliases("navy"));
        TagRegistry.INSTANCE.register((new DeferredTag("formatting", -1, -1, TagFormatting.DarkGreen::new)).setName("darkgreen"));
        TagRegistry.INSTANCE.register((new DeferredTag("formatting", -1, -1, TagFormatting.DarkAqua::new)).setName("darkaqua").setAliases("darkcyan", "turquoise"));
        TagRegistry.INSTANCE.register((new DeferredTag("formatting", -1, -1, TagFormatting.DarkRed::new)).setName("darkred"));
        TagRegistry.INSTANCE.register((new DeferredTag("formatting", -1, -1, TagFormatting.DarkPurple::new)).setName("darkpurple").setAliases("purple", "violet"));
        TagRegistry.INSTANCE.register((new DeferredTag("formatting", -1, -1, TagFormatting.Gold::new)).setName("gold").setAliases("orange"));
        TagRegistry.INSTANCE.register((new DeferredTag("formatting", -1, -1, TagFormatting.Gray::new)).setName("gray").setAliases("grey", "lightgray", "lightgrey"));
        TagRegistry.INSTANCE.register((new DeferredTag("formatting", -1, -1, TagFormatting.DarkGray::new)).setName("darkgrey").setAliases("darkgray", "charcoal"));
        TagRegistry.INSTANCE.register((new DeferredTag("formatting", -1, -1, TagFormatting.Blue::new)).setName("blue").setAliases("lightblue", "indigo"));
        TagRegistry.INSTANCE.register((new DeferredTag("formatting", -1, -1, TagFormatting.Green::new)).setName("green").setAliases("brightgreen", "lightgreen", "lime"));
        TagRegistry.INSTANCE.register((new DeferredTag("formatting", -1, -1, TagFormatting.Aqua::new)).setName("aqua").setAliases("cyan", "celeste", "diamond"));
        TagRegistry.INSTANCE.register((new DeferredTag("formatting", -1, -1, TagFormatting.Red::new)).setName("red").setAliases("lightred", "salmon"));
        TagRegistry.INSTANCE.register((new DeferredTag("formatting", -1, -1, TagFormatting.LightPurple::new)).setName("lightpurple").setAliases("magenta", "pink"));
        TagRegistry.INSTANCE.register((new DeferredTag("formatting", -1, -1, TagFormatting.Yellow::new)).setName("yellow"));
        TagRegistry.INSTANCE.register((new DeferredTag("formatting", -1, -1, TagFormatting.White::new)).setName("white"));
        TagRegistry.INSTANCE.register((new DeferredTag("formatting", -1, -1, TagFormatting.Obfuscated::new)).setName("obfuscated").setAliases("random"));
        TagRegistry.INSTANCE.register((new DeferredTag("formatting", -1, -1, TagFormatting.Bold::new)).setName("bold").setAliases("b"));
        TagRegistry.INSTANCE.register((new DeferredTag("formatting", -1, -1, TagFormatting.Strikethrough::new)).setName("strikethrough").setAliases("strike", "s"));
        TagRegistry.INSTANCE.register((new DeferredTag("formatting", -1, -1, TagFormatting.Underline::new)).setName("underline").setAliases("u"));
        TagRegistry.INSTANCE.register((new DeferredTag("formatting", -1, -1, TagFormatting.Italic::new)).setName("italic").setAliases("italics", "i"));
        TagRegistry.INSTANCE.register((new DeferredTag("formatting", -1, -1, TagFormatting.Reset::new)).setName("reset").setAliases("r"));
    }

    public static void registerMisc() {
        TagRegistry.INSTANCE.register((new DeferredTag("misc", -1, -1, TagMisc.MemoryMaximum::new)).setName("memmax"));
        TagRegistry.INSTANCE.register((new DeferredTag("misc", -1, -1, TagMisc.MemoryTotal::new)).setName("memtotal"));
        TagRegistry.INSTANCE.register((new DeferredTag("misc", -1, -1, TagMisc.MemoryFree::new)).setName("memfree"));
        TagRegistry.INSTANCE.register((new DeferredTag("misc", -1, -1, TagMisc.MemoryUsed::new)).setName("memused"));
        TagRegistry.INSTANCE.register((new DeferredTag("misc", -1, -1, TagMisc.FPS::new)).setName("fps"));
        TagRegistry.INSTANCE.register((new DeferredTag("misc", -1, -1, TagMisc.ResourcePack::new)).setName("resourcepack"));
        TagRegistry.INSTANCE.register((new DeferredTag("misc", -1, -1, TagMisc.EntitiesRendered::new)).setName("entitiesrendered"));
        TagRegistry.INSTANCE.register((new DeferredTag("misc", -1, -1, TagMisc.EntitiesTotal::new)).setName("entitiestotal"));
        TagRegistry.INSTANCE.register((new DeferredTag("misc", -1, -1, TagMisc.LoadedChunks::new)).setName("loadedchunks"));
        TagRegistry.INSTANCE.register((new DeferredTag("misc", -1, -1, TagMisc.Server::new)).setName("server"));
        TagRegistry.INSTANCE.register((new DeferredTag("misc", -1, -1, TagMisc.ServerName::new)).setName("servername"));
        TagRegistry.INSTANCE.register((new DeferredTag("misc", -1, -1, TagMisc.ServerIP::new)).setName("serverip"));
        TagRegistry.INSTANCE.register((new DeferredTag("misc", -1, -1, TagMisc.ServerPort::new)).setName("serverport"));
        TagRegistry.INSTANCE.register((new DeferredTag("misc", -1, -1, TagMisc.Ping::new)).setName("ping"));
        TagRegistry.INSTANCE.register((new DeferredTag("misc", -1, -1, TagMisc.PingIcon::new)).setName("pingicon"));
    }

    public static void registerMouseOver() {
        TagRegistry.INSTANCE.register((new DeferredTag("mouseover", -1, -1, TagMouseOver.Name::new)).setName("mouseovername"));
        TagRegistry.INSTANCE.register((new DeferredTag("mouseover", -1, -1, TagMouseOver.UniqueName::new)).setName("mouseoveruniquename"));
        TagRegistry.INSTANCE.register((new DeferredTag("mouseover", -1, -1, TagMouseOver.Id::new)).setName("mouseoverid"));
        TagRegistry.INSTANCE.register((new DeferredTag("mouseover", -1, -1, TagMouseOver.Metadata::new)).setName("mouseovermetadata"));
        TagRegistry.INSTANCE.register((new DeferredTag("mouseover", -1, -1, TagMouseOver.PowerWeak::new)).setName("mouseoverpowerweak"));
        TagRegistry.INSTANCE.register((new DeferredTag("mouseover", -1, -1, TagMouseOver.PowerStrong::new)).setName("mouseoverpowerstrong"));
        TagRegistry.INSTANCE.register((new DeferredTag("mouseover", -1, -1, TagMouseOver.PowerInput::new)).setName("mouseoverpowerinput"));
    }

    public static void registerNearbyPlayer() {
        for (int i = 0; i < 16; ++i) {
            final int index = i;
            TagRegistry.INSTANCE.register((new DeferredTag("nearbyplayer", index, 15, () -> new TagNearbyPlayer.Name(index))).setName("nearbyplayername"));
            TagRegistry.INSTANCE.register((new DeferredTag("nearbyplayer", index, 15, () -> new TagNearbyPlayer.Distance(index))).setName("nearbyplayerdistance"));
            TagRegistry.INSTANCE.register((new DeferredTag("nearbyplayer", index, 15, () -> new TagNearbyPlayer.Icon(index))).setName("nearbyplayericon"));
        }

    }

    public static void registerPlayerEquipment() {
        for (int i = 0; i < TagPlayerEquipment.TYPES.length; ++i) {
            final int slot = TagPlayerEquipment.SLOTS[i];
            TagRegistry.INSTANCE.register((new DeferredTag("playerequipment", -1, -1, () -> new TagPlayerEquipment.Name(slot))).setName(TagPlayerEquipment.TYPES[i] + "name"));
            TagRegistry.INSTANCE.register((new DeferredTag("playerequipment", -1, -1, () -> new TagPlayerEquipment.UniqueName(slot))).setName(TagPlayerEquipment.TYPES[i] + "uniquename"));
            TagRegistry.INSTANCE.register((new DeferredTag("playerequipment", -1, -1, () -> new TagPlayerEquipment.Damage(slot))).setName(TagPlayerEquipment.TYPES[i] + "damage"));
            TagRegistry.INSTANCE.register((new DeferredTag("playerequipment", -1, -1, () -> new TagPlayerEquipment.MaximumDamage(slot))).setName(TagPlayerEquipment.TYPES[i] + "maxdamage"));
            TagRegistry.INSTANCE.register((new DeferredTag("playerequipment", -1, -1, () -> new TagPlayerEquipment.DamageLeft(slot))).setName(TagPlayerEquipment.TYPES[i] + "damageleft"));
            TagRegistry.INSTANCE.register((new DeferredTag("playerequipment", -1, -1, () -> new TagPlayerEquipment.Quantity(slot))).setName(TagPlayerEquipment.TYPES[i] + "quantity"));
            TagRegistry.INSTANCE.register((new DeferredTag("playerequipment", -1, -1, () -> new TagPlayerEquipment.Icon(slot, false))).setName(TagPlayerEquipment.TYPES[i] + "icon"));
            TagRegistry.INSTANCE.register((new DeferredTag("playerequipment", -1, -1, () -> new TagPlayerEquipment.Icon(slot, true))).setName(TagPlayerEquipment.TYPES[i] + "largeicon"));
        }

    }

    public static void registerPlayerGeneral() {
        TagRegistry.INSTANCE.register((new DeferredTag("playergeneral", -1, -1, TagPlayerGeneral.Light::new)).setName("light"));
        TagRegistry.INSTANCE.register((new DeferredTag("playergeneral", -1, -1, TagPlayerGeneral.LightEye::new)).setName("lighteye"));
        TagRegistry.INSTANCE.register((new DeferredTag("playergeneral", -1, -1, TagPlayerGeneral.LightNoSun::new)).setName("lightnosun"));
        TagRegistry.INSTANCE.register((new DeferredTag("playergeneral", -1, -1, TagPlayerGeneral.LightNoSunEye::new)).setName("lightnosuneye"));
        TagRegistry.INSTANCE.register((new DeferredTag("playergeneral", -1, -1, TagPlayerGeneral.LightSun::new)).setName("lightsun"));
        TagRegistry.INSTANCE.register((new DeferredTag("playergeneral", -1, -1, TagPlayerGeneral.LightSunEye::new)).setName("lightsuneye"));
        TagRegistry.INSTANCE.register((new DeferredTag("playergeneral", -1, -1, TagPlayerGeneral.Score::new)).setName("score"));
        TagRegistry.INSTANCE.register((new DeferredTag("playergeneral", -1, -1, TagPlayerGeneral.GameMode::new)).setName("gamemode"));
        TagRegistry.INSTANCE.register((new DeferredTag("playergeneral", -1, -1, TagPlayerGeneral.GameModeId::new)).setName("gamemodeid"));
        TagRegistry.INSTANCE.register((new DeferredTag("playergeneral", -1, -1, TagPlayerGeneral.Health::new)).setName("health").setAliases("healthpoints"));
        TagRegistry.INSTANCE.register((new DeferredTag("playergeneral", -1, -1, TagPlayerGeneral.MaxHealth::new)).setName("maxhealth").setAliases("maxhealthpoints"));
        TagRegistry.INSTANCE.register((new DeferredTag("playergeneral", -1, -1, TagPlayerGeneral.Armor::new)).setName("armor").setAliases("armorpoints"));
        TagRegistry.INSTANCE.register((new DeferredTag("playergeneral", -1, -1, TagPlayerGeneral.FoodLevel::new)).setName("foodlevel").setAliases("foodpoints"));
        TagRegistry.INSTANCE.register((new DeferredTag("playergeneral", -1, -1, TagPlayerGeneral.MaxFoodLevel::new)).setName("maxfoodlevel").setAliases("maxfoodpoints"));
        TagRegistry.INSTANCE.register((new DeferredTag("playergeneral", -1, -1, TagPlayerGeneral.Saturation::new)).setName("saturation").setAliases("foodsaturation"));
        TagRegistry.INSTANCE.register((new DeferredTag("playergeneral", -1, -1, TagPlayerGeneral.Exhaustion::new)).setName("exhaustion").setAliases("foodexhaustion"));
        TagRegistry.INSTANCE.register((new DeferredTag("playergeneral", -1, -1, TagPlayerGeneral.AirTicks::new)).setName("airticks"));
        TagRegistry.INSTANCE.register((new DeferredTag("playergeneral", -1, -1, TagPlayerGeneral.MaxAirTicks::new)).setName("maxairticks"));
        TagRegistry.INSTANCE.register((new DeferredTag("playergeneral", -1, -1, TagPlayerGeneral.PlayerLevel::new)).setName("playerlevel"));
        TagRegistry.INSTANCE.register((new DeferredTag("playergeneral", -1, -1, TagPlayerGeneral.CurrentExperience::new)).setName("xpthislevel"));
        TagRegistry.INSTANCE.register((new DeferredTag("playergeneral", -1, -1, TagPlayerGeneral.ExperienceUntilNext::new)).setName("xpuntilnext"));
        TagRegistry.INSTANCE.register((new DeferredTag("playergeneral", -1, -1, TagPlayerGeneral.ExperienceCap::new)).setName("xpcap"));
        TagRegistry.INSTANCE.register((new DeferredTag("playergeneral", -1, -1, TagPlayerGeneral.Username::new)).setName("username"));
        TagRegistry.INSTANCE.register((new DeferredTag("playergeneral", -1, -1, TagPlayerGeneral.InWater::new)).setName("inwater").setAliases("underwater"));
        TagRegistry.INSTANCE.register((new DeferredTag("playergeneral", -1, -1, TagPlayerGeneral.Wet::new)).setName("wet"));
        TagRegistry.INSTANCE.register((new DeferredTag("playergeneral", -1, -1, TagPlayerGeneral.Alive::new)).setName("alive"));
        TagRegistry.INSTANCE.register((new DeferredTag("playergeneral", -1, -1, TagPlayerGeneral.Burning::new)).setName("burning"));
        TagRegistry.INSTANCE.register((new DeferredTag("playergeneral", -1, -1, TagPlayerGeneral.Riding::new)).setName("riding"));
        TagRegistry.INSTANCE.register((new DeferredTag("playergeneral", -1, -1, TagPlayerGeneral.Sneaking::new)).setName("sneaking"));
        TagRegistry.INSTANCE.register((new DeferredTag("playergeneral", -1, -1, TagPlayerGeneral.Sprinting::new)).setName("sprinting"));
        TagRegistry.INSTANCE.register((new DeferredTag("playergeneral", -1, -1, TagPlayerGeneral.Invisible::new)).setName("invisible"));
        TagRegistry.INSTANCE.register((new DeferredTag("playergeneral", -1, -1, TagPlayerGeneral.Eating::new)).setName("eating"));
    }

    public static void registerPlayerPosition() {
        TagRegistry.INSTANCE.register((new DeferredTag("playerposition", -1, -1, TagPlayerPosition.ChunkX::new)).setName("chunkx"));
        TagRegistry.INSTANCE.register((new DeferredTag("playerposition", -1, -1, TagPlayerPosition.ChunkZ::new)).setName("chunkz"));
        TagRegistry.INSTANCE.register((new DeferredTag("playerposition", -1, -1, TagPlayerPosition.ChunkOffsetX::new)).setName("chunkoffsetx"));
        TagRegistry.INSTANCE.register((new DeferredTag("playerposition", -1, -1, TagPlayerPosition.ChunkOffsetZ::new)).setName("chunkoffsetz"));
        TagRegistry.INSTANCE.register((new DeferredTag("playerposition", -1, -1, TagPlayerPosition.X::new)).setName("x").setAliases("xr"));
        TagRegistry.INSTANCE.register((new DeferredTag("playerposition", -1, -1, TagPlayerPosition.Y::new)).setName("y").setAliases("yr"));
        TagRegistry.INSTANCE.register((new DeferredTag("playerposition", -1, -1, TagPlayerPosition.YEye::new)).setName("yeye").setAliases("yeyer"));
        TagRegistry.INSTANCE.register((new DeferredTag("playerposition", -1, -1, TagPlayerPosition.Z::new)).setName("z").setAliases("zr"));
        TagRegistry.INSTANCE.register((new DeferredTag("playerposition", -1, -1, TagPlayerPosition.Xi::new)).setName("xi"));
        TagRegistry.INSTANCE.register((new DeferredTag("playerposition", -1, -1, TagPlayerPosition.Yi::new)).setName("yi"));
        TagRegistry.INSTANCE.register((new DeferredTag("playerposition", -1, -1, TagPlayerPosition.YEyei::new)).setName("yeyei"));
        TagRegistry.INSTANCE.register((new DeferredTag("playerposition", -1, -1, TagPlayerPosition.Zi::new)).setName("zi"));
        TagRegistry.INSTANCE.register((new DeferredTag("playerposition", -1, -1, TagPlayerPosition.Speed::new)).setName("speed"));
        TagRegistry.INSTANCE.register((new DeferredTag("playerposition", -1, -1, TagPlayerPosition.SpeedX::new)).setName("speedx"));
        TagRegistry.INSTANCE.register((new DeferredTag("playerposition", -1, -1, TagPlayerPosition.SpeedY::new)).setName("speedy"));
        TagRegistry.INSTANCE.register((new DeferredTag("playerposition", -1, -1, TagPlayerPosition.SpeedZ::new)).setName("speedz"));
        TagRegistry.INSTANCE.register((new DeferredTag("playerposition", -1, -1, TagPlayerPosition.SpeedXZ::new)).setName("speedxz"));
        TagRegistry.INSTANCE.register((new DeferredTag("playerposition", -1, -1, TagPlayerPosition.Pitch::new)).setName("pitch"));
        TagRegistry.INSTANCE.register((new DeferredTag("playerposition", -1, -1, TagPlayerPosition.Direction::new)).setName("direction"));
        TagRegistry.INSTANCE.register((new DeferredTag("playerposition", -1, -1, TagPlayerPosition.RoughDirection::new)).setName("roughdirection"));
        TagRegistry.INSTANCE.register((new DeferredTag("playerposition", -1, -1, TagPlayerPosition.FineDirection::new)).setName("finedirection"));
        TagRegistry.INSTANCE.register((new DeferredTag("playerposition", -1, -1, TagPlayerPosition.AbbreviatedRoughDirection::new)).setName("abrroughdirection"));
        TagRegistry.INSTANCE.register((new DeferredTag("playerposition", -1, -1, TagPlayerPosition.AbbreviatedFineDirection::new)).setName("abrfinedirection"));
        TagRegistry.INSTANCE.register((new DeferredTag("playerposition", -1, -1, TagPlayerPosition.AxisDirection::new)).setName("axisdirection"));
        TagRegistry.INSTANCE.register((new DeferredTag("playerposition", -1, -1, TagPlayerPosition.DirectionHud::new)).setName("directionhud"));
    }

    public static void registerPlayerPotion() {
        for (int i = 0; i < 32; ++i) {
            final int index = i;
            TagRegistry.INSTANCE.register((new DeferredTag("playerpotion", index, 31, () -> new TagPlayerPotion.Effect(index))).setName("potioneffect"));
            TagRegistry.INSTANCE.register((new DeferredTag("playerpotion", index, 31, () -> new TagPlayerPotion.Duration(index))).setName("potionduration"));
            TagRegistry.INSTANCE.register((new DeferredTag("playerpotion", index, 31, () -> new TagPlayerPotion.DurationTicks(index))).setName("potiondurationticks"));
            TagRegistry.INSTANCE.register((new DeferredTag("playerpotion", index, 31, () -> new TagPlayerPotion.Negative(index))).setName("potionnegative"));
            TagRegistry.INSTANCE.register((new DeferredTag("playerpotion", index, 31, () -> new TagPlayerPotion.Icon(index, false))).setName("potionicon"));
            TagRegistry.INSTANCE.register((new DeferredTag("playerpotion", index, 31, () -> new TagPlayerPotion.Icon(index, true))).setName("potionlargeicon"));
        }

    }

    public static void registerRiding() {
        TagRegistry.INSTANCE.register((new DeferredTag("riding", -1, -1, TagRiding.IsHorse::new)).setName("ridinghorse"));
        TagRegistry.INSTANCE.register((new DeferredTag("riding", -1, -1, TagRiding.HorseHealth::new)).setName("horsehealth"));
        TagRegistry.INSTANCE.register((new DeferredTag("riding", -1, -1, TagRiding.HorseMaxHealth::new)).setName("horsemaxhealth"));
        TagRegistry.INSTANCE.register((new DeferredTag("riding", -1, -1, TagRiding.HorseSpeed::new)).setName("horsespeed"));
        TagRegistry.INSTANCE.register((new DeferredTag("riding", -1, -1, TagRiding.HorseJump::new)).setName("horsejumpstrength").setAliases("horsejumpstr"));
    }

    public static void registerTime() {
        TagRegistry.INSTANCE.register((new DeferredTag("time", -1, -1, TagTime.MinecraftDay::new)).setName("day"));
        TagRegistry.INSTANCE.register((new DeferredTag("time", -1, -1, TagTime.MinecraftHour::new)).setName("mctimeh"));
        TagRegistry.INSTANCE.register((new DeferredTag("time", -1, -1, TagTime.MinecraftMinute::new)).setName("mctimem"));
        TagRegistry.INSTANCE.register((new DeferredTag("time", -1, -1, TagTime.Minecraft24::new)).setName("mctime24").setAliases("mctime"));
        TagRegistry.INSTANCE.register((new DeferredTag("time", -1, -1, TagTime.Minecraft12::new)).setName("mctime12"));
        TagRegistry.INSTANCE.register((new TagTime.Real24()).setName("rltime24").setAliases("rltime"));
        TagRegistry.INSTANCE.register((new TagTime.Real12()).setName("rltime12"));
    }

    public static void registerWorld() {
        TagRegistry.INSTANCE.register((new DeferredTag("world", -1, -1, TagWorld.Name::new)).setName("worldname"));
        TagRegistry.INSTANCE.register((new DeferredTag("world", -1, -1, TagWorld.Size::new)).setName("worldsize"));
        TagRegistry.INSTANCE.register((new DeferredTag("world", -1, -1, TagWorld.SizeMB::new)).setName("worldsizemb"));
        TagRegistry.INSTANCE.register((new DeferredTag("world", -1, -1, TagWorld.Seed::new)).setName("seed"));
        TagRegistry.INSTANCE.register((new DeferredTag("world", -1, -1, TagWorld.Difficulty::new)).setName("difficulty"));
        TagRegistry.INSTANCE.register((new DeferredTag("world", -1, -1, TagWorld.DifficultyId::new)).setName("difficultyid"));
        TagRegistry.INSTANCE.register((new DeferredTag("world", -1, -1, TagWorld.LocalDifficulty::new)).setName("localdifficulty"));
        TagRegistry.INSTANCE.register((new DeferredTag("world", -1, -1, TagWorld.Dimension::new)).setName("dimension"));
        TagRegistry.INSTANCE.register((new DeferredTag("world", -1, -1, TagWorld.DimensionId::new)).setName("dimensionid"));
        TagRegistry.INSTANCE.register((new DeferredTag("world", -1, -1, TagWorld.Biome::new)).setName("biome"));
        TagRegistry.INSTANCE.register((new DeferredTag("world", -1, -1, TagWorld.BiomeId::new)).setName("biomeid"));
        TagRegistry.INSTANCE.register((new DeferredTag("world", -1, -1, TagWorld.Daytime::new)).setName("daytime"));
        TagRegistry.INSTANCE.register((new DeferredTag("world", -1, -1, TagWorld.MoonPhase::new)).setName("moonphase"));
        TagRegistry.INSTANCE.register((new DeferredTag("world", -1, -1, TagWorld.Raining::new)).setName("raining"));
        TagRegistry.INSTANCE.register((new DeferredTag("world", -1, -1, TagWorld.Thundering::new)).setName("thundering"));
        TagRegistry.INSTANCE.register((new DeferredTag("world", -1, -1, TagWorld.Snowing::new)).setName("snowing"));
        TagRegistry.INSTANCE.register((new DeferredTag("world", -1, -1, TagWorld.NextWeatherChange::new)).setName("nextweatherchange").setAliases("nextrain"));
        TagRegistry.INSTANCE.register((new DeferredTag("world", -1, -1, TagWorld.Slimes::new)).setName("slimes"));
        TagRegistry.INSTANCE.register((new DeferredTag("world", -1, -1, TagWorld.SlimeChunk::new)).setName("slimechunk"));
        TagRegistry.INSTANCE.register((new DeferredTag("world", -1, -1, TagWorld.Hardcore::new)).setName("hardcore"));
        TagRegistry.INSTANCE.register((new DeferredTag("world", -1, -1, TagWorld.Temperature::new)).setName("temperature"));
        TagRegistry.INSTANCE.register((new DeferredTag("world", -1, -1, TagWorld.LocalTemperature::new)).setName("localtemperature"));
        TagRegistry.INSTANCE.register((new DeferredTag("world", -1, -1, TagWorld.Humidity::new)).setName("humidity"));
    }

}
