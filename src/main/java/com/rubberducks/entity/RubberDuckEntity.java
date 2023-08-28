package com.rubberducks.entity;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.rubberducks.RubberDucks;

import net.minecraft.block.BlockState;
import net.minecraft.block.LilyPadBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockLocating;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

public class RubberDuckEntity extends Entity {
  public Identifier texture = new Identifier("rubberducks", "textures/entity/rubber_duck/rubber_duck.png");

  public static final float WATER_VELOCITY_DECAY = 0.99f;
  public static final float LAND_VELOCITY_DECAY = 0.6f;
  public static final float UNDER_FLOWING_WATER_VELOCITY_DECAY = 0.9f;
  public static final float UNDER_WATER_VELOCITY_DECAY = 0.45f;
  public static final float IN_AIR_VELOCITY_DECAY = 0.9f;

  private static final TrackedData<Integer> DAMAGE_WOBBLE_TICKS = DataTracker.registerData(BoatEntity.class,
      TrackedDataHandlerRegistry.INTEGER);
  private static final TrackedData<Integer> DAMAGE_WOBBLE_SIDE = DataTracker.registerData(BoatEntity.class,
      TrackedDataHandlerRegistry.INTEGER);
  private static final TrackedData<Float> DAMAGE_WOBBLE_STRENGTH = DataTracker.registerData(BoatEntity.class,
      TrackedDataHandlerRegistry.FLOAT);
  // private static final TrackedData<Integer> BOAT_TYPE =
  // DataTracker.registerData(BoatEntity.class,
  // TrackedDataHandlerRegistry.INTEGER);
  private static final TrackedData<Integer> BUBBLE_WOBBLE_TICKS = DataTracker.registerData(BoatEntity.class,
      TrackedDataHandlerRegistry.INTEGER);

  private float velocityDecay;
  private int velocityInterval;
  private double x;
  private double y;
  private double z;
  private double yaw;
  private double boatPitch;
  private double waterLevel;
  private float nearbySlipperiness;
  private Location location;
  private Location lastLocation;
  private double fallVelocity;
  private boolean onBubbleColumnSurface;
  private boolean bubbleColumnIsDrag;
  private float bubbleWobbleStrength;
  private float bubbleWobble;
  private float lastBubbleWobble;

  public RubberDuckEntity(EntityType<RubberDuckEntity> type, World world) {
    super(type, world);
    this.intersectionChecked = true;
  }

  @Override
  protected Entity.MoveEffect getMoveEffect() {
    return Entity.MoveEffect.EVENTS;
  }

  @Override
  protected void initDataTracker() {
    this.dataTracker.startTracking(DAMAGE_WOBBLE_TICKS, 0);
    this.dataTracker.startTracking(DAMAGE_WOBBLE_SIDE, 1);
    this.dataTracker.startTracking(DAMAGE_WOBBLE_STRENGTH, Float.valueOf(0.0f));
    // this.dataTracker.startTracking(BOAT_TYPE, Type.OAK.ordinal());
    this.dataTracker.startTracking(BUBBLE_WOBBLE_TICKS, 0);
  }

  @Override
  public boolean collidesWith(Entity other) {
    return BoatEntity.canCollide(this, other);
  }

  public static boolean canCollide(Entity entity, Entity other) {
    return (other.isCollidable() || other.isPushable()) && !entity.isConnectedThroughVehicle(other);
  }

  @Override
  public boolean isCollidable() {
    return true;
  }

  @Override
  public boolean isPushable() {
    return true;
  }

  @Override
  protected Vec3d positionInPortal(Direction.Axis portalAxis, BlockLocating.Rectangle portalRect) {
    return LivingEntity.positionInPortal(super.positionInPortal(portalAxis, portalRect));
  }

  @Override
  public boolean damage(DamageSource source, float amount) {
    boolean bl;

    if (this.isInvulnerableTo(source)) {
      return false;
    }

    if (this.getWorld().isClient || this.isRemoved()) {
      return true;
    }

    this.setDamageWobbleSide(-this.getDamageWobbleSide());
    this.setDamageWobbleTicks(10);
    this.setDamageWobbleStrength(this.getDamageWobbleStrength() + amount * 10.0f);
    this.scheduleVelocityUpdate();
    this.emitGameEvent(GameEvent.ENTITY_DAMAGE, source.getAttacker());

    boolean bl2 = bl = source.getAttacker() instanceof PlayerEntity
        && ((PlayerEntity) source.getAttacker()).getAbilities().creativeMode;

    if (bl || this.getDamageWobbleStrength() > 40.0f) {
      if (!bl && this.getWorld().getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS)) {
        // this.dropItems(source);
      }
      this.discard();
    }
    return true;
  }

  // protected void dropItems(DamageSource source) {
  // this.dropItem(this.asItem());
  // }

  @Override
  public void onBubbleColumnSurfaceCollision(boolean drag) {
    if (!this.getWorld().isClient) {
      this.onBubbleColumnSurface = true;
      this.bubbleColumnIsDrag = drag;
      if (this.getBubbleWobbleTicks() == 0) {
        this.setBubbleWobbleTicks(60);
      }
    }

    this.getWorld().addParticle(ParticleTypes.SPLASH, this.getX() + (double) this.random.nextFloat(), this.getY() + 0.7,
        this.getZ() + (double) this.random.nextFloat(), 0.0, 0.0, 0.0);

    if (this.random.nextInt(20) == 0) {
      this.getWorld().playSound(this.getX(), this.getY(), this.getZ(), this.getSplashSound(), this.getSoundCategory(),
          1.0f, 0.8f + 0.4f * this.random.nextFloat(), false);
      this.emitGameEvent(GameEvent.SPLASH, this.getControllingPassenger());
    }
  }

  @Override
  public void pushAwayFrom(Entity entity) {
    if (entity instanceof BoatEntity) {
      if (entity.getBoundingBox().minY < this.getBoundingBox().maxY) {
        super.pushAwayFrom(entity);
      }
    } else if (entity.getBoundingBox().minY <= this.getBoundingBox().minY) {
      super.pushAwayFrom(entity);
    }
  }

  // public Item asItem() {
  // return switch (this.getVariant()) {
  // case Type.SPRUCE -> Items.SPRUCE_BOAT;
  // case Type.BIRCH -> Items.BIRCH_BOAT;
  // case Type.JUNGLE -> Items.JUNGLE_BOAT;
  // case Type.ACACIA -> Items.ACACIA_BOAT;
  // case Type.CHERRY -> Items.CHERRY_BOAT;
  // case Type.DARK_OAK -> Items.DARK_OAK_BOAT;
  // case Type.MANGROVE -> Items.MANGROVE_BOAT;
  // case Type.BAMBOO -> Items.BAMBOO_RAFT;
  // default -> Items.OAK_BOAT;
  // };
  // }

  @Override
  public void animateDamage(float yaw) {
    this.setDamageWobbleSide(-this.getDamageWobbleSide());
    this.setDamageWobbleTicks(10);
    this.setDamageWobbleStrength(this.getDamageWobbleStrength() * 11.0f);
  }

  @Override
  public boolean canHit() {
    return !this.isRemoved();
  }

  @Override
  public void updateTrackedPositionAndAngles(double x, double y, double z, float yaw, float pitch,
      int interpolationSteps, boolean interpolate) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.yaw = yaw;
    this.boatPitch = pitch;
    this.velocityInterval = 10;
  }

  @Override
  public Direction getMovementDirection() {
    return this.getHorizontalFacing().rotateYClockwise();
  }

  @Override
  public void tick() {
    this.lastLocation = this.location;
    this.location = this.checkLocation();

    if (this.getDamageWobbleTicks() > 0) {
      this.setDamageWobbleTicks(this.getDamageWobbleTicks() - 1);
    }

    if (this.getDamageWobbleStrength() > 0.0f) {
      this.setDamageWobbleStrength(this.getDamageWobbleStrength() - 1.0f);
    }

    super.tick();
    this.updatePositionAndRotation();

    if (this.isLogicalSideForUpdatingMovement()) {
      this.updateVelocity();
      this.move(MovementType.SELF, this.getVelocity());
    } else {
      this.setVelocity(Vec3d.ZERO);
    }

    this.handleBubbleColumn();

    this.checkBlockCollision();

    List<Entity> list = this.getWorld().getOtherEntities(this, this.getBoundingBox().expand(0.2f, -0.01f, 0.2f),
        EntityPredicates.canBePushedBy(this));
    if (!list.isEmpty()) {
      for (int j = 0; j < list.size(); ++j) {
        Entity entity = list.get(j);
        this.pushAwayFrom(entity);
      }
    }
  }

  private void handleBubbleColumn() {
    if (this.getWorld().isClient) {
      int i = this.getBubbleWobbleTicks();
      this.bubbleWobbleStrength = i > 0 ? (this.bubbleWobbleStrength += 0.05f) : (this.bubbleWobbleStrength -= 0.1f);
      this.bubbleWobbleStrength = MathHelper.clamp(this.bubbleWobbleStrength, 0.0f, 1.0f);
      this.lastBubbleWobble = this.bubbleWobble;
      this.bubbleWobble = 10.0f * (float) Math.sin(0.5f * (float) this.getWorld().getTime())
          * this.bubbleWobbleStrength;
    } else {
      int i;
      if (!this.onBubbleColumnSurface) {
        this.setBubbleWobbleTicks(0);
      }
      if ((i = this.getBubbleWobbleTicks()) > 0) {
        this.setBubbleWobbleTicks(--i);
        int j = 60 - i - 1;
        if (j > 0 && i == 0) {
          this.setBubbleWobbleTicks(0);
          Vec3d vec3d = this.getVelocity();
          if (this.bubbleColumnIsDrag) {
            this.setVelocity(vec3d.add(0.0, -0.7, 0.0));
            this.removeAllPassengers();
          } else {
            this.setVelocity(vec3d.x, this.hasPassenger((Entity entity) -> entity instanceof PlayerEntity) ? 2.7 : 0.6,
                vec3d.z);
          }
        }
        this.onBubbleColumnSurface = false;
      }
    }
  }

  private void updatePositionAndRotation() {
    if (this.isLogicalSideForUpdatingMovement()) {
      this.velocityInterval = 0;
      this.updateTrackedPosition(this.getX(), this.getY(), this.getZ());
    }

    if (this.velocityInterval <= 0)
      return;

    double newX = this.getX() + (this.x - this.getX()) / (double) this.velocityInterval;
    double newY = this.getY() + (this.y - this.getY()) / (double) this.velocityInterval;
    double newZ = this.getZ() + (this.z - this.getZ()) / (double) this.velocityInterval;
    double newYaw = MathHelper.wrapDegrees(this.yaw - (double) this.getYaw());

    this.setYaw(this.getYaw() + (float) newYaw / (float) this.velocityInterval);
    this.setPitch(
        this.getPitch() + (float) (this.boatPitch - (double) this.getPitch()) / (float) this.velocityInterval);

    --this.velocityInterval;

    this.setPosition(newX, newY, newZ);
    this.setRotation(this.getYaw(), this.getPitch());
  }

  private Location checkLocation() {
    Location location = this.getUnderWaterLocation();
    if (location != null) {
      this.waterLevel = this.getBoundingBox().maxY;
      return location;
    }
    if (this.checkBoatInWater()) {
      return Location.IN_WATER;
    }
    float f = this.getNearbySlipperiness();
    if (f > 0.0f) {
      this.nearbySlipperiness = f;
      return Location.ON_LAND;
    }
    return Location.IN_AIR;
  }

  public float getWaterHeightBelow() {
    Box box = this.getBoundingBox();
    int i = MathHelper.floor(box.minX);
    int j = MathHelper.ceil(box.maxX);
    int k = MathHelper.floor(box.maxY);
    int l = MathHelper.ceil(box.maxY - this.fallVelocity);
    int m = MathHelper.floor(box.minZ);
    int n = MathHelper.ceil(box.maxZ);
    BlockPos.Mutable mutable = new BlockPos.Mutable();
    block0: for (int o = k; o < l; ++o) {
      float f = 0.0f;
      for (int p = i; p < j; ++p) {
        for (int q = m; q < n; ++q) {
          mutable.set(p, o, q);
          FluidState fluidState = this.getWorld().getFluidState(mutable);
          if (fluidState.isIn(FluidTags.WATER)) {
            f = Math.max(f, fluidState.getHeight(this.getWorld(), mutable));
          }
          if (f >= 1.0f)
            continue block0;
        }
      }
      if (!(f < 1.0f))
        continue;
      return (float) mutable.getY() + f;
    }
    return l + 1;
  }

  public float getNearbySlipperiness() {
    Box box = this.getBoundingBox();
    Box box2 = new Box(box.minX, box.minY - 0.001, box.minZ, box.maxX, box.minY, box.maxZ);
    int i = MathHelper.floor(box2.minX) - 1;
    int j = MathHelper.ceil(box2.maxX) + 1;
    int k = MathHelper.floor(box2.minY) - 1;
    int l = MathHelper.ceil(box2.maxY) + 1;
    int m = MathHelper.floor(box2.minZ) - 1;
    int n = MathHelper.ceil(box2.maxZ) + 1;
    VoxelShape voxelShape = VoxelShapes.cuboid(box2);
    float f = 0.0f;
    int o = 0;
    BlockPos.Mutable mutable = new BlockPos.Mutable();
    for (int p = i; p < j; ++p) {
      for (int q = m; q < n; ++q) {
        int r = (p == i || p == j - 1 ? 1 : 0) + (q == m || q == n - 1 ? 1 : 0);
        if (r == 2)
          continue;
        for (int s = k; s < l; ++s) {
          if (r > 0 && (s == k || s == l - 1))
            continue;
          mutable.set(p, s, q);
          BlockState blockState = this.getWorld().getBlockState(mutable);
          if (blockState.getBlock() instanceof LilyPadBlock
              || !VoxelShapes.matchesAnywhere(blockState.getCollisionShape(this.getWorld(), mutable).offset(p, s, q),
                  voxelShape, BooleanBiFunction.AND))
            continue;
          f += blockState.getBlock().getSlipperiness();
          ++o;
        }
      }
    }
    return f / (float) o;
  }

  private boolean checkBoatInWater() {
    Box box = this.getBoundingBox();
    int i = MathHelper.floor(box.minX);
    int j = MathHelper.ceil(box.maxX);
    int k = MathHelper.floor(box.minY);
    int l = MathHelper.ceil(box.minY + 0.001);
    int m = MathHelper.floor(box.minZ);
    int n = MathHelper.ceil(box.maxZ);
    boolean bl = false;
    this.waterLevel = -1.7976931348623157E308;
    BlockPos.Mutable mutable = new BlockPos.Mutable();
    for (int o = i; o < j; ++o) {
      for (int p = k; p < l; ++p) {
        for (int q = m; q < n; ++q) {
          mutable.set(o, p, q);
          FluidState fluidState = this.getWorld().getFluidState(mutable);
          if (!fluidState.isIn(FluidTags.WATER))
            continue;
          float f = (float) p + fluidState.getHeight(this.getWorld(), mutable);
          this.waterLevel = Math.max((double) f, this.waterLevel);
          bl |= box.minY < (double) f;
        }
      }
    }
    return bl;
  }

  @Nullable
  private Location getUnderWaterLocation() {
    Box box = this.getBoundingBox();
    double d = box.maxY + 0.001;
    int i = MathHelper.floor(box.minX);
    int j = MathHelper.ceil(box.maxX);
    int k = MathHelper.floor(box.maxY);
    int l = MathHelper.ceil(d);
    int m = MathHelper.floor(box.minZ);
    int n = MathHelper.ceil(box.maxZ);
    boolean bl = false;
    BlockPos.Mutable mutable = new BlockPos.Mutable();
    for (int o = i; o < j; ++o) {
      for (int p = k; p < l; ++p) {
        for (int q = m; q < n; ++q) {
          mutable.set(o, p, q);
          FluidState fluidState = this.getWorld().getFluidState(mutable);
          if (!fluidState.isIn(FluidTags.WATER)
              || !(d < (double) ((float) mutable.getY() + fluidState.getHeight(this.getWorld(), mutable))))
            continue;
          if (fluidState.isStill()) {
            bl = true;
            continue;
          }
          return Location.UNDER_FLOWING_WATER;
        }
      }
    }
    return bl ? Location.UNDER_WATER : null;
  }

  private void updateVelocity() {
    double d = -0.04f;
    double e = this.hasNoGravity() ? 0.0 : d;
    double f = 0.0; // Vertical velocity: > 1 = up, < 1 = down
    this.velocityDecay = 0.05f;

    // Entering water from the air
    if (this.lastLocation == Location.IN_AIR && this.location != Location.IN_AIR && this.location != Location.ON_LAND) {
      this.waterLevel = this.getBodyY(1.0);
      this.setPosition(this.getX(), (double) (this.getWaterHeightBelow() - this.getHeight()) + 0.101, this.getZ());
      this.setVelocity(this.getVelocity().multiply(1.0, 0.0, 1.0));
      this.fallVelocity = 0.0;
      this.location = Location.IN_WATER;
    } else {
      if (this.location == Location.IN_WATER) {
        f = (this.waterLevel - this.getY()) / (double) this.getHeight();
        this.velocityDecay = WATER_VELOCITY_DECAY;
      } else if (this.location == Location.UNDER_FLOWING_WATER) {
        e = -7.0E-4;
        this.velocityDecay = UNDER_FLOWING_WATER_VELOCITY_DECAY;
      } else if (this.location == Location.UNDER_WATER) {
        // Rise back to the surface when underwater
        f = 2.0f;
        this.velocityDecay = UNDER_WATER_VELOCITY_DECAY;
      } else if (this.location == Location.IN_AIR) {
        this.velocityDecay = IN_AIR_VELOCITY_DECAY;
      } else if (this.location == Location.ON_LAND) {
        this.velocityDecay = this.nearbySlipperiness;
      }

      Vec3d vec3d = this.getVelocity();
      this.setVelocity(vec3d.x * (double) this.velocityDecay, vec3d.y + e, vec3d.z * (double) this.velocityDecay);

      if (f > 0.0) {
        Vec3d vec3d2 = this.getVelocity();
        this.setVelocity(vec3d2.x, (vec3d2.y + f * 0.06153846016296973) * 0.75, vec3d2.z);
      }
    }
  }

  @Override
  protected void writeCustomDataToNbt(NbtCompound nbt) {
    // nbt.putString("Type", this.getVariant().asString());
  }

  @Override
  protected void readCustomDataFromNbt(NbtCompound nbt) {
    // if (nbt.contains("Type", NbtElement.STRING_TYPE)) {
    // this.setVariant(Type.getType(nbt.getString("Type")));
    // }
  }

  @Override
  public ActionResult interact(PlayerEntity player, Hand hand) {
    if (player.shouldCancelInteraction()) {
      return ActionResult.PASS;
    }

    this.pushAwayFrom(player);
    this.getWorld().playSound(this.getX(), this.getY(), this.getZ(), RubberDucks.SQUEAK1_EVENT,
        this.getSoundCategory(), 1.0f, 0.8f + 0.4f * this.random.nextFloat(), false);

    return ActionResult.SUCCESS;
  }

  @Override
  protected void fall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition) {
    this.fallVelocity = this.getVelocity().y;
    if (this.hasVehicle()) {
      return;
    }

    if (onGround) {
      this.onLanding();
    } else if (!this.getWorld().getFluidState(this.getBlockPos().down()).isIn(FluidTags.WATER)
        && heightDifference < 0.0) {
      this.fallDistance -= (float) heightDifference;
    }
  }

  public void setDamageWobbleStrength(float wobbleStrength) {
    this.dataTracker.set(DAMAGE_WOBBLE_STRENGTH, Float.valueOf(wobbleStrength));
  }

  public float getDamageWobbleStrength() {
    return this.dataTracker.get(DAMAGE_WOBBLE_STRENGTH).floatValue();
  }

  public void setDamageWobbleTicks(int wobbleTicks) {
    this.dataTracker.set(DAMAGE_WOBBLE_TICKS, wobbleTicks);
  }

  public int getDamageWobbleTicks() {
    return this.dataTracker.get(DAMAGE_WOBBLE_TICKS);
  }

  private void setBubbleWobbleTicks(int wobbleTicks) {
    this.dataTracker.set(BUBBLE_WOBBLE_TICKS, wobbleTicks);
  }

  private int getBubbleWobbleTicks() {
    return this.dataTracker.get(BUBBLE_WOBBLE_TICKS);
  }

  public float interpolateBubbleWobble(float tickDelta) {
    return MathHelper.lerp(tickDelta, this.lastBubbleWobble, this.bubbleWobble);
  }

  public void setDamageWobbleSide(int side) {
    this.dataTracker.set(DAMAGE_WOBBLE_SIDE, side);
  }

  public int getDamageWobbleSide() {
    return this.dataTracker.get(DAMAGE_WOBBLE_SIDE);
  }

  // @Override
  // public void setVariant(Type type) {
  // this.dataTracker.set(BOAT_TYPE, type.ordinal());
  // }

  // @Override
  // public Type getVariant() {
  // return Type.getType(this.dataTracker.get(BOAT_TYPE));
  // }

  @Override
  public boolean isSubmergedInWater() {
    return this.location == Location.UNDER_WATER || this.location == Location.UNDER_FLOWING_WATER;
  }

  // @Override
  // public ItemStack getPickBlockStack() {
  // return new ItemStack(this.asItem());
  // }

  // @Override
  // public /* synthetic */ Object getVariant() {
  // return this.getVariant();
  // }

  // public static enum Type implements StringIdentifiable
  // {
  // OAK(Blocks.OAK_PLANKS, "oak"),
  // SPRUCE(Blocks.SPRUCE_PLANKS, "spruce"),
  // BIRCH(Blocks.BIRCH_PLANKS, "birch"),
  // JUNGLE(Blocks.JUNGLE_PLANKS, "jungle"),
  // ACACIA(Blocks.ACACIA_PLANKS, "acacia"),
  // CHERRY(Blocks.CHERRY_PLANKS, "cherry"),
  // DARK_OAK(Blocks.DARK_OAK_PLANKS, "dark_oak"),
  // MANGROVE(Blocks.MANGROVE_PLANKS, "mangrove"),
  // BAMBOO(Blocks.BAMBOO_PLANKS, "bamboo");

  // private final String name;
  // private final Block baseBlock;
  // public static final StringIdentifiable.Codec<Type> CODEC;
  // private static final IntFunction<Type> BY_ID;

  // private Type(Block baseBlock, String name) {
  // this.name = name;
  // this.baseBlock = baseBlock;
  // }

  // @Override
  // public String asString() {
  // return this.name;
  // }

  // public String getName() {
  // return this.name;
  // }

  // public Block getBaseBlock() {
  // return this.baseBlock;
  // }

  // public String toString() {
  // return this.name;
  // }

  // public static Type getType(int type) {
  // return BY_ID.apply(type);
  // }

  // public static Type getType(String name) {
  // return CODEC.byId(name, OAK);
  // }

  // static {
  // CODEC = StringIdentifiable.createCodec(Type::values);
  // BY_ID = ValueLists.createIdToValueFunction(Enum::ordinal, Type.values(),
  // ValueLists.OutOfBoundsHandling.ZERO);
  // }
  // }

  public static enum Location {
    IN_WATER,
    UNDER_WATER,
    UNDER_FLOWING_WATER,
    ON_LAND,
    IN_AIR;
  }
}
