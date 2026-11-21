package github.kasuminova.novaeng.common.performance;

import github.kasuminova.novaeng.client.geometry.item.ItemSpriteCompatibility;
import github.kasuminova.novaeng.common.util.EntityLivingBaseValues;
import github.kasuminova.novaeng.testutil.ForgeReflectionEnvironment;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Bootstrap;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftReflectionCompatibilityTest {
    @BeforeAll
    static void initializeMinecraft() {
        ForgeReflectionEnvironment.initialize();
        Bootstrap.register();
    }

    @Test
    void everyGeometryReaderOverrideAndInheritedOverrideRetainsFallback() {
        assertTrue(ItemSpriteCompatibility.hasVanillaReads(new Sprite()));
        assertTrue(ItemSpriteCompatibility.hasVanillaReads(new Sprite() {
        }));
        final TextureAtlasSprite[] customized = {
            new Sprite() {
                @Override
                public int getIconWidth() {
                    throw new AssertionError();
                }
            },
            new Sprite() {
                @Override
                public int getIconHeight() {
                    throw new AssertionError();
                }
            },
            new Sprite() {
                @Override
                public int getFrameCount() {
                    throw new AssertionError();
                }
            },
            new Sprite() {
                @Override
                public int[][] getFrameTextureData(int frame) {
                    throw new AssertionError();
                }
            },
            new CustomUvSprite(), new CustomUvSprite() {
        },
            new Sprite() {
                @Override
                public float getMaxU() {
                    throw new AssertionError();
                }
            },
            new Sprite() {
                @Override
                public float getMinV() {
                    throw new AssertionError();
                }
            },
            new Sprite() {
                @Override
                public float getMaxV() {
                    throw new AssertionError();
                }
            },
            new Sprite() {
                @Override
                public float getInterpolatedU(double u) {
                    throw new AssertionError();
                }
            },
            new Sprite() {
                @Override
                public float getInterpolatedV(double v) {
                    throw new AssertionError();
                }
            }
        };
        for (final TextureAtlasSprite sprite : customized) {
            assertFalse(ItemSpriteCompatibility.hasVanillaReads(sprite), sprite.getClass().getName());
            assertFalse(ItemSpriteCompatibility.hasVanillaReads(sprite), "The cached answer must agree");
        }
    }

    @Test
    void mappedBlockReadersKeepDefaultOverloadAndCustomDispatchDecisions() {
        final PlainBlock plain = new PlainBlock();
        final PlainBlock inherited = new PlainBlock() {
        };
        final PlainBlock overloaded = new OverloadedBlock();
        for (final PlainBlock block : new PlainBlock[]{plain, inherited, overloaded}) {
            assertTrue(PrimerImportMethods.supports(block));
            assertEquals(PathNodeType.BLOCKED, StablePathNodeTypes.get(block.getDefaultState()));
        }
        for (final PlainBlock block : new PlainBlock[]{new CustomMaterial(), new CustomMaterial() {
        }, new CustomTick()})
            assertFalse(PrimerImportMethods.supports(block));
        for (final PlainBlock block : new PlainBlock[]{new CustomMaterial(), new CustomMaterial() {
        }, new CustomPassable(), new CustomPathType()})
            assertNull(StablePathNodeTypes.get(block.getDefaultState()), block.getClass().getName());
        assertEquals(PathNodeType.BLOCKED, StablePathNodeTypes.get(new CustomTick().getDefaultState()),
            "An unrelated override must not disable stable path classification");
    }

    @Test
    void healthFieldResolvesThroughTheGeneratedFieldMapping() {
        assertNotNull(EntityLivingBaseValues.HEALTH);
        assertSame(DataSerializers.FLOAT, EntityLivingBaseValues.HEALTH.getSerializer());
    }

    private static class Sprite extends TextureAtlasSprite {
        Sprite() {
            super("reflection-compatibility");
        }
    }

    private static class CustomUvSprite extends Sprite {
        @Override
        public float getMinU() {
            throw new AssertionError("Detection must not invoke getters");
        }
    }

    private static class PlainBlock extends Block {
        PlainBlock() {
            super(Material.ROCK);
        }
    }

    private static class CustomMaterial extends PlainBlock {
        @Override
        public Material getMaterial(IBlockState state) {
            return Material.ROCK;
        }
    }

    private static class CustomTick extends PlainBlock {
        @Override
        public boolean getTickRandomly() {
            return false;
        }
    }

    private static class CustomPassable extends PlainBlock {
        @Override
        public boolean isPassable(IBlockAccess world, BlockPos pos) {
            return false;
        }
    }

    private static class CustomPathType extends PlainBlock {
        @Override
        public PathNodeType getAiPathNodeType(IBlockState state, IBlockAccess world, BlockPos pos, EntityLiving entity) {
            return PathNodeType.WALKABLE;
        }
    }

    private static class OverloadedBlock extends PlainBlock {
        public Material getMaterial() {
            return Material.ROCK;
        }

        public boolean isPassable(BlockPos pos) {
            return false;
        }
    }
}
