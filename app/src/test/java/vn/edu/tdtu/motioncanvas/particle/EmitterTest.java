package vn.edu.tdtu.motioncanvas.particle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Random;

import vn.edu.tdtu.motioncanvas.physics.Body;
import vn.edu.tdtu.motioncanvas.physics.World;

public class EmitterTest {
    private static final float FRAME = 1f / 60f;
    private static final float EPS = 0.001f;

    private Emitter emitter(int capacity) {
        return new Emitter(capacity, 1f, new Random(42));
    }

    @Test
    public void fireRateAccumulatesAcrossFrames() {
        Emitter emitter = emitter(100);
        for (int i = 0; i < 6; i++) {
            emitter.update(FRAME);
        }
        assertEquals(30, emitter.aliveCount());
    }

    @Test
    public void fractionalFramesDoNotLoseParticlesToRounding() {
        Emitter emitter = emitter(100);
        for (int i = 0; i < 100; i++) {
            emitter.update(0.001f);
        }
        assertEquals(30, emitter.aliveCount());
    }

    @Test
    public void fullPoolDropsEmissionInsteadOfSavingABacklog() {
        Emitter emitter = emitter(10);
        emitter.burst(0f, 0f, 1000);
        assertEquals(10, emitter.aliveCount());
        for (int i = 0; i < 10; i++) {
            emitter.update(FRAME);
        }
        for (int i = 0; i < emitter.capacity(); i++) {
            emitter.get(i).age = emitter.get(i).life;
        }
        emitter.update(FRAME);
        assertEquals(5, emitter.aliveCount());
    }

    @Test
    public void stoppingEmissionLetsExistingFireExpire() {
        Emitter emitter = emitter(100);
        emitter.update(FRAME);
        emitter.setEmitting(false);
        for (int i = 0; i < 90; i++) {
            emitter.update(FRAME);
        }
        assertEquals(0, emitter.aliveCount());
    }

    @Test
    public void burstWorksWithEmissionOffAndAlwaysCreatesFire() {
        Emitter emitter = emitter(30);
        emitter.setMode(Emitter.Mode.WATER);
        emitter.setEmitting(false);
        emitter.burst(120f, 250f, 20);
        assertEquals(20, emitter.aliveCount());
        for (int i = 0; i < 20; i++) {
            Particle p = emitter.get(i);
            assertSame(Emitter.Mode.FIRE, p.mode);
            assertFalse(p.splash);
            assertEquals(120f, p.x, 0f);
            assertEquals(250f, p.y, 0f);
            double speed = Math.hypot(p.vx, p.vy);
            assertTrue(speed >= 80f && speed <= 260f);
        }
    }

    @Test
    public void expiredSlotsReuseTheSameObjects() {
        Emitter emitter = emitter(2);
        emitter.setEmitting(false);
        Particle first = emitter.get(0);
        Particle second = emitter.get(1);
        emitter.burst(0f, 0f, 2);
        for (int i = 0; i < 60; i++) {
            emitter.update(FRAME);
        }
        assertEquals(0, emitter.aliveCount());
        emitter.burst(60f, 70f, 2);
        assertSame(first, emitter.get(0));
        assertSame(second, emitter.get(1));
        assertEquals(2, emitter.aliveCount());
        assertEquals(0f, first.age, 0f);
        assertEquals(60f, first.x, 0f);
    }

    @Test
    public void zeroCapacityAndNonpositiveBurstAreHarmless() {
        Emitter empty = emitter(0);
        empty.update(FRAME);
        empty.burst(0f, 0f, 10);
        assertEquals(0, empty.aliveCount());
        Emitter emitter = emitter(10);
        emitter.burst(0f, 0f, 0);
        emitter.burst(0f, 0f, -4);
        assertEquals(0, emitter.aliveCount());
    }

    @Test
    public void changingModeKeepsThePhysicsOfExistingParticles() {
        Emitter emitter = emitter(20);
        emitter.update(FRAME);
        Particle fire = emitter.get(0);
        float previousVy = fire.vy;
        emitter.setMode(Emitter.Mode.WATER);
        emitter.update(FRAME);
        assertSame(Emitter.Mode.WATER, emitter.getMode());
        assertSame(Emitter.Mode.FIRE, fire.mode);
        assertEquals(previousVy - 120f * FRAME, fire.vy, EPS);
        assertSame(Emitter.Mode.WATER, emitter.get(5).mode);
    }

    @Test
    public void waterFallsAndCreatesSplashWithoutManualCollisionSetup() {
        Emitter emitter = emitter(100);
        emitter.setMode(Emitter.Mode.WATER);
        emitter.setPosition(50f, 0f);
        emitter.setFloor(10f);
        emitter.update(0.004f);
        emitter.setEmitting(false);
        boolean sawSplash = false;
        for (int frame = 0; frame < 120; frame++) {
            emitter.update(FRAME);
            for (int i = 0; i < emitter.capacity(); i++) {
                if (emitter.get(i).alive && emitter.get(i).splash) {
                    sawSplash = true;
                }
            }
        }
        assertTrue(sawSplash);
    }

    private Emitter waterAboutToHitFloor(int capacity) {
        Emitter emitter = emitter(capacity);
        emitter.setMode(Emitter.Mode.WATER);
        emitter.setFloor(100f);
        emitter.update(0.004f);
        emitter.setEmitting(false);
        Particle drop = emitter.get(0);
        drop.x = 30f;
        drop.y = 99f;
        drop.vx = 0f;
        drop.vy = 300f;
        return emitter;
    }

    @Test
    public void newSplashesStartTogetherRegardlessOfPoolIndex() {
        Emitter emitter = waterAboutToHitFloor(4);
        emitter.update(FRAME);
        assertEquals(3, emitter.aliveCount());
        for (int i = 0; i < emitter.capacity(); i++) {
            Particle p = emitter.get(i);
            if (!p.alive) continue;
            assertTrue(p.splash);
            assertEquals(0f, p.age, 0f);
            assertEquals(30f, p.x, 0f);
            assertEquals(100f, p.y, 0f);
        }
        emitter.update(FRAME);
        for (int i = 0; i < emitter.capacity(); i++) {
            Particle p = emitter.get(i);
            if (!p.alive) continue;
            assertEquals(FRAME, p.age, EPS);
            assertTrue(p.y < 100f);
        }
    }

    @Test
    public void waterImpactCanReuseItsOwnSlotInAFullPool() {
        Emitter emitter = waterAboutToHitFloor(1);
        Particle original = emitter.get(0);
        emitter.update(FRAME);
        assertSame(original, emitter.get(0));
        assertEquals(1, emitter.aliveCount());
        assertTrue(original.splash);
        assertEquals(30f, original.x, 0f);
        assertEquals(0f, original.age, 0f);
    }

    @Test
    public void splashDiesAtFloorWithoutSpawningAnotherGeneration() {
        Emitter emitter = waterAboutToHitFloor(4);
        emitter.update(FRAME);
        for (int i = 0; i < emitter.capacity(); i++) {
            Particle p = emitter.get(i);
            if (p.alive) {
                p.y = 99f;
                p.vy = 300f;
            }
        }
        emitter.update(FRAME);
        assertEquals(0, emitter.aliveCount());
    }

    @Test
    public void longFrameMatchesOneClampedStep() {
        Emitter longFrame = emitter(100);
        Emitter clamped = emitter(100);
        longFrame.update(FRAME);
        clamped.update(FRAME);
        longFrame.update(5f);
        clamped.update(Emitter.MAX_DT);
        assertSameState(clamped, longFrame);
    }

    @Test
    public void invalidTimeDoesNotChangeStateOrConsumeRandomNumbers() {
        Emitter actual = emitter(30);
        Emitter expected = emitter(30);
        actual.update(FRAME);
        expected.update(FRAME);
        actual.update(0f);
        actual.update(-1f);
        actual.update(Float.NaN);
        assertSameState(expected, actual);
        actual.update(FRAME);
        expected.update(FRAME);
        assertSameState(expected, actual);
    }

    @Test
    public void densityScalesDistanceAndVelocityButNotLifetime() {
        Emitter normal = new Emitter(20, 1f, new Random(7));
        Emitter doubled = new Emitter(20, 2f, new Random(7));
        normal.setPosition(50f, 60f);
        doubled.setPosition(100f, 120f);
        normal.update(FRAME);
        doubled.update(FRAME);
        normal.setEmitting(false);
        doubled.setEmitting(false);
        normal.update(FRAME);
        doubled.update(FRAME);
        for (int i = 0; i < normal.aliveCount(); i++) {
            Particle a = normal.get(i);
            Particle b = doubled.get(i);
            assertEquals(a.x * 2f, b.x, EPS);
            assertEquals(a.y * 2f, b.y, EPS);
            assertEquals(a.vx * 2f, b.vx, EPS);
            assertEquals(a.vy * 2f, b.vy, EPS);
            assertEquals(a.size * 2f, b.size, EPS);
            assertEquals(a.life, b.life, 0f);
        }
    }

    @Test
    public void waterSimulationKeepsAccurateCountsAndFiniteState() {
        Emitter emitter = emitter(80);
        emitter.setMode(Emitter.Mode.WATER);
        emitter.setFloor(200f);
        for (int frame = 0; frame < 600; frame++) {
            emitter.update(FRAME);
            int count = 0;
            for (int i = 0; i < emitter.capacity(); i++) {
                Particle p = emitter.get(i);
                if (!p.alive) continue;
                count++;
                assertTrue(Float.isFinite(p.x));
                assertTrue(Float.isFinite(p.y));
                assertTrue(Float.isFinite(p.vx));
                assertTrue(Float.isFinite(p.vy));
                assertTrue(p.progress() >= 0f && p.progress() <= 1f);
            }
            assertEquals(count, emitter.aliveCount());
            assertTrue(count <= emitter.capacity());
        }
    }

    @Test
    public void progressStaysWithinLifetimeBounds() {
        Particle particle = new Particle();
        assertEquals(1f, particle.progress(), 0f);
        particle.life = 2f;
        particle.age = -1f;
        assertEquals(0f, particle.progress(), 0f);
        particle.age = 1f;
        assertEquals(0.5f, particle.progress(), 0f);
        particle.age = 3f;
        assertEquals(1f, particle.progress(), 0f);
    }

    @Test
    public void paletteUsesArgbAndClampsAge() {
        assertEquals(0xffffdc3c, ParticlePalette.fire(0f));
        assertEquals(0x00ff0000, ParticlePalette.fire(1f));
        assertEquals(0xdc5aaaff, ParticlePalette.water(0f));
        assertEquals(0x425aaaff, ParticlePalette.water(1f));
        assertEquals(ParticlePalette.fire(0f), ParticlePalette.fire(-2f));
        assertEquals(ParticlePalette.water(1f), ParticlePalette.water(2f));
        int previousAlpha = 255;
        for (int i = 0; i <= 100; i++) {
            int alpha = ParticlePalette.fire(i / 100f) >>> 24;
            assertTrue(alpha <= previousAlpha);
            previousAlpha = alpha;
        }
    }

    @Test
    public void worldCollisionCanDriveBurstWithContinuousEmissionOff() {
        World world = new World(200f, 200f, 1);
        world.setGravity(0f);
        Emitter emitter = emitter(30);
        emitter.setEmitting(false);
        world.setCollisionListener((x, y, speed) -> emitter.burst(x, y, 14));
        Body ball = new Body(100f, 189f, 10f, 0);
        ball.vy = 600f;
        world.addBody(ball);
        world.step(FRAME);
        assertEquals(14, emitter.aliveCount());
        assertEquals(100f, emitter.get(0).x, EPS);
        assertEquals(200f, emitter.get(0).y, EPS);
    }

    private void assertSameState(Emitter expected, Emitter actual) {
        assertEquals(expected.aliveCount(), actual.aliveCount());
        for (int i = 0; i < expected.capacity(); i++) {
            Particle a = expected.get(i);
            Particle b = actual.get(i);
            assertEquals(a.alive, b.alive);
            assertEquals(a.splash, b.splash);
            assertSame(a.mode, b.mode);
            assertEquals(a.x, b.x, 0f);
            assertEquals(a.y, b.y, 0f);
            assertEquals(a.vx, b.vx, 0f);
            assertEquals(a.vy, b.vy, 0f);
            assertEquals(a.age, b.age, 0f);
            assertEquals(a.life, b.life, 0f);
            assertEquals(a.size, b.size, 0f);
        }
    }
}
