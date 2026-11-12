package com.vladik.godbox.client;

import com.vladik.godbox.GodParticles;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Клиентская часть частиц взрыва. Параметры приходят через «скорость» пакета (count = 0):
 * fireball: vx = размер, vy = время жизни; shockwave: vx = радиус, vy = время жизни;
 * plume: vx = размер, vy = подъём, vz = время жизни; ember/debris — обычная скорость.
 */
public final class GodParticlesClient {
    private static final int FULL_BRIGHT = 0xF000F0;

    private GodParticlesClient() {}

    public static void init() {
        ParticleFactoryRegistry r = ParticleFactoryRegistry.getInstance();
        r.register(GodParticles.FIREBALL, sp -> (t, w, x, y, z, vx, vy, vz) -> new Fireball(w, x, y, z, vx, vy, sp));
        r.register(GodParticles.SHOCKWAVE, sp -> (t, w, x, y, z, vx, vy, vz) -> new Shockwave(w, x, y, z, vx, vy, sp));
        r.register(GodParticles.PLUME, sp -> (t, w, x, y, z, vx, vy, vz) -> new Plume(w, x, y, z, vx, vy, vz, sp));
        r.register(GodParticles.EMBER, sp -> (t, w, x, y, z, vx, vy, vz) -> new Ember(w, x, y, z, vx, vy, vz, sp));
        r.register(GodParticles.DEBRIS, sp -> (t, w, x, y, z, vx, vy, vz) -> new Debris(w, x, y, z, vx, vy, vz, sp));
    }

    private static float frac(int age, int max) {
        return max <= 0 ? 1f : Math.min(1f, (float) age / max);
    }

    /** Многокадровый огненный шар, светится в темноте. */
    static final class Fireball extends SpriteBillboardParticle {
        private final SpriteProvider sprites;
        private final float size0;

        Fireball(ClientWorld w, double x, double y, double z, double size, double life, SpriteProvider sp) {
            super(w, x, y, z);
            sprites = sp;
            size0 = (float) MathHelper.clamp(size, 0.2, 40);
            scale = size0;
            maxAge = MathHelper.clamp((int) life, 4, 60);
            collidesWithWorld = false;
            velocityY = 0.012 + size0 * 0.004;
            setSpriteForAge(sp);
        }

        @Override
        public void tick() {
            prevPosX = x;
            prevPosY = y;
            prevPosZ = z;
            if (age++ >= maxAge) {
                markDead();
                return;
            }
            setSpriteForAge(sprites);
            y += velocityY;
            float k = frac(age, maxAge);
            alpha = k < 0.65f ? 1f : Math.max(0f, 1f - (k - 0.65f) / 0.35f);
        }

        @Override
        public float getSize(float td) {
            float k = frac(age, maxAge);
            return size0 * (0.75f + 0.35f * (float) Math.sin(Math.min(1f, k * 1.6f) * Math.PI / 2));
        }

        @Override
        protected int getBrightness(float td) {
            return FULL_BRIGHT;
        }

        @Override
        public ParticleTextureSheet getType() {
            return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    /** Плоское кольцо ударной волны на земле: своя геометрия, не билборд. */
    static final class Shockwave extends SpriteBillboardParticle {
        private final SpriteProvider sprites;
        private final float radius;

        Shockwave(ClientWorld w, double x, double y, double z, double radius, double life, SpriteProvider sp) {
            super(w, x, y, z);
            sprites = sp;
            this.radius = (float) MathHelper.clamp(radius, 0.5, 60);
            maxAge = MathHelper.clamp((int) life, 4, 50);
            collidesWithWorld = false;
            setSpriteForAge(sp);
        }

        @Override
        public void tick() {
            prevPosX = x;
            prevPosY = y;
            prevPosZ = z;
            if (age++ >= maxAge) {
                markDead();
                return;
            }
            setSpriteForAge(sprites);
        }

        @Override
        public void buildGeometry(VertexConsumer vc, Camera camera, float td) {
            float t = Math.min(1f, (age + td) / maxAge);
            float ease = 1f - (1f - t) * (1f - t) * (1f - t);
            float s = Math.max(0.1f, radius * ease);
            float a = 0.92f * (float) Math.pow(1f - t, 1.3);
            if (a <= 0.01f) return;
            Vec3d cam = camera.getPos();
            float cx = (float) (MathHelper.lerp(td, prevPosX, x) - cam.x);
            float cy = (float) (MathHelper.lerp(td, prevPosY, y) - cam.y);
            float cz = (float) (MathHelper.lerp(td, prevPosZ, z) - cam.z);
            float u0 = getMinU(), u1 = getMaxU(), v0 = getMinV(), v1 = getMaxV();
            int l = getBrightness(td);
            // оба направления обхода: видно и сверху, и снизу независимо от отсечения граней
            float[][] c = {{-s, -s, u0, v0}, {-s, s, u0, v1}, {s, s, u1, v1}, {s, -s, u1, v0}};
            for (int i = 0; i < 4; i++) {
                float[] p = c[i];
                vc.vertex(cx + p[0], cy, cz + p[1]).texture(p[2], p[3]).color(red, green, blue, a).light(l);
            }
            for (int i = 3; i >= 0; i--) {
                float[] p = c[i];
                vc.vertex(cx + p[0], cy, cz + p[1]).texture(p[2], p[3]).color(red, green, blue, a).light(l);
            }
        }

        @Override
        public ParticleTextureSheet getType() {
            return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    /** Тёмный столб дыма: поднимается, медленно растёт, сносится ветром и тает. */
    static final class Plume extends SpriteBillboardParticle {
        private final float size0;
        private final float spin;

        Plume(ClientWorld w, double x, double y, double z, double size, double rise, double life, SpriteProvider sp) {
            super(w, x, y, z);
            size0 = (float) MathHelper.clamp(size, 0.3, 14);
            scale = size0;
            maxAge = MathHelper.clamp((int) life, 10, 400);
            collidesWithWorld = false;
            velocityY = rise;
            velocityX = 0.012 + (random.nextDouble() - 0.5) * 0.01;
            velocityZ = 0.006 + (random.nextDouble() - 0.5) * 0.01;
            float g = 0.8f + random.nextFloat() * 0.2f;
            setColor(g, g, g);
            spin = (random.nextFloat() - 0.5f) * 0.03f;
            angle = random.nextFloat() * 6.28f;
            setSprite(sp);
        }

        @Override
        public void tick() {
            prevPosX = x;
            prevPosY = y;
            prevPosZ = z;
            prevAngle = angle;
            if (age++ >= maxAge) {
                markDead();
                return;
            }
            x += velocityX;
            y += velocityY;
            z += velocityZ;
            velocityY *= 0.985;
            angle += spin;
            float k = frac(age, maxAge);
            alpha = k < 0.5f ? Math.min(1f, k * 8f) * 0.92f : 0.92f * (1f - (k - 0.5f) / 0.5f);
        }

        @Override
        public float getSize(float td) {
            return size0 * (1f + 0.6f * frac(age, maxAge));
        }

        @Override
        public ParticleTextureSheet getType() {
            return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    /** Искры/угольки: светятся, падают, отскакивают и остывают по кадрам. */
    static final class Ember extends SpriteBillboardParticle {
        private final SpriteProvider sprites;

        Ember(ClientWorld w, double x, double y, double z, double vx, double vy, double vz, SpriteProvider sp) {
            super(w, x, y, z);
            sprites = sp;
            velocityX = vx;
            velocityY = vy;
            velocityZ = vz;
            scale = 0.09f + random.nextFloat() * 0.07f;
            maxAge = 30 + random.nextInt(40);
            gravityStrength = 0.9f;
            collidesWithWorld = true;
            setBoundingBoxSpacing(0.05f, 0.05f);
            setSpriteForAge(sp);
        }

        @Override
        public void tick() {
            prevPosX = x;
            prevPosY = y;
            prevPosZ = z;
            if (age++ >= maxAge) {
                markDead();
                return;
            }
            setSpriteForAge(sprites);
            velocityY -= 0.04 * gravityStrength;
            move(velocityX, velocityY, velocityZ);
            velocityX *= 0.97;
            velocityY *= 0.98;
            velocityZ *= 0.97;
            if (onGround) {
                velocityX *= 0.6;
                velocityZ *= 0.6;
                velocityY = Math.abs(velocityY) * 0.35;
            }
        }

        @Override
        protected int getBrightness(float td) {
            return frac(age, maxAge) < 0.7f ? FULL_BRIGHT : super.getBrightness(td);
        }

        @Override
        public ParticleTextureSheet getType() {
            return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    /** Летящие обломки и пыль. */
    static final class Debris extends SpriteBillboardParticle {
        private final float spin;

        Debris(ClientWorld w, double x, double y, double z, double vx, double vy, double vz, SpriteProvider sp) {
            super(w, x, y, z);
            velocityX = vx;
            velocityY = vy;
            velocityZ = vz;
            scale = 0.07f + random.nextFloat() * 0.1f;
            maxAge = 28 + random.nextInt(30);
            gravityStrength = 1f;
            collidesWithWorld = true;
            setBoundingBoxSpacing(0.06f, 0.06f);
            float g = 0.45f + random.nextFloat() * 0.4f;
            setColor(g * 1.05f, g, g * 0.9f);
            spin = (random.nextFloat() - 0.5f) * 0.5f;
            angle = random.nextFloat() * 6.28f;
            setSprite(sp);
        }

        @Override
        public void tick() {
            prevPosX = x;
            prevPosY = y;
            prevPosZ = z;
            prevAngle = angle;
            if (age++ >= maxAge) {
                markDead();
                return;
            }
            velocityY -= 0.04 * gravityStrength;
            move(velocityX, velocityY, velocityZ);
            velocityX *= 0.96;
            velocityY *= 0.98;
            velocityZ *= 0.96;
            if (onGround) {
                velocityX *= 0.5;
                velocityZ *= 0.5;
            } else {
                angle += spin;
            }
            alpha = age > maxAge - 8 ? Math.max(0f, (maxAge - age) / 8f) : 1f;
        }

        @Override
        public ParticleTextureSheet getType() {
            return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
        }
    }
}
