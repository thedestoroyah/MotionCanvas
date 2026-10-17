package vn.edu.tdtu.motioncanvas.physics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class WorldTest {
    private static final float DT = 1f / 60f;
    private static final float EPS = 0.001f;

    private World world(int capacity) {
        World result = new World(2000f, 2000f, capacity);
        result.setGravity(0f);
        return result;
    }

    @Test
    public void freeFallFollowsAccelerationAndDisplacement() {
        World world = world(1);
        world.setGravity(600f);
        Body ball = new Body(200f, 200f, 12f, 0);
        world.addBody(ball);
        for (int i = 0; i < 60; i++) world.step(DT);

        assertEquals(600f, ball.vy, 0.01f);
        // y = y0 + g*t*t/2, Euler bán ẩn có sai số tích phân nhỏ
        assertEquals(500f, ball.y, 2f);
    }

    @Test
    public void allFourWallsReverseOnlyTheNormalVelocity() {
        float[][] starts = {
                {12f, 200f, -600f, 30f}, {1988f, 200f, 600f, 30f},
                {200f, 12f, 30f, -600f}, {200f, 1988f, 30f, 600f}
        };
        for (int i = 0; i < starts.length; i++) {
            World world = world(1);
            world.setRestitution(0.5f);
            float[] start = starts[i];
            Body ball = new Body(start[0], start[1], 10f, 0);
            ball.vx = start[2];
            ball.vy = start[3];
            world.addBody(ball);
            world.step(DT);
            assertEquals(i < 2 ? -start[2] * 0.5f : start[2], ball.vx, EPS);
            assertEquals(i < 2 ? start[3] : -start[3] * 0.5f, ball.vy, EPS);
            assertTrue(ball.x >= 10f && ball.x <= 1990f);
            assertTrue(ball.y >= 10f && ball.y <= 1990f);
        }
    }

    @Test
    public void wallEventHasContactPositionAndIncomingSpeed() {
        World world = world(1);
        Body ball = new Body(300f, 1989f, 10f, 0);
        ball.vy = 600f;
        world.addBody(ball);
        int[] events = {0};
        float[] contact = new float[3];
        world.setCollisionListener((x, y, speed) -> {
            events[0]++;
            contact[0] = x;
            contact[1] = y;
            contact[2] = speed;
        });
        world.step(DT);
        assertEquals(1, events[0]);
        assertEquals(300f, contact[0], EPS);
        assertEquals(2000f, contact[1], EPS);
        assertEquals(600f, contact[2], EPS);
    }

    @Test
    public void collisionThresholdIncludesTheBoundary() {
        for (float speed : new float[]{149f, World.MIN_IMPACT_SPEED}) {
            World world = world(1);
            Body ball = new Body(100f, 1990f, 10f, 0);
            ball.vy = speed;
            world.addBody(ball);
            int[] events = {0};
            world.setCollisionListener((x, y, impact) -> events[0]++);
            world.step(DT);
            assertEquals(speed < World.MIN_IMPACT_SPEED ? 0 : 1, events[0]);
        }
    }

    @Test
    public void restingBallDoesNotKeepReportingImpacts() {
        World world = world(1);
        world.setGravity(2000f);
        world.addBody(new Body(100f, 1990f, 10f, 0));
        int[] events = {0};
        world.setCollisionListener((x, y, speed) -> events[0]++);
        for (int i = 0; i < 120; i++) world.step(DT);
        assertEquals(0, events[0]);
    }

    @Test
    public void unequalMassElasticCollisionConservesMomentumAndEnergy() {
        World world = world(2);
        world.setRestitution(1f);
        Body a = new Body(400f, 400f, 10f, 0);
        Body b = new Body(440f, 400f, 20f, 0);
        a.vx = 300f;
        b.vx = -100f;
        world.addBody(a);
        world.addBody(b);
        float momentum = a.mass * a.vx + b.mass * b.vx;
        double energy = energy(a) + energy(b);
        int[] events = {0};
        world.setCollisionListener((x, y, speed) -> {
            events[0]++;
            assertEquals(400f, speed, EPS);
        });
        world.step(World.MAX_DT);

        assertEquals(momentum, a.mass * a.vx + b.mass * b.vx, 0.1f);
        assertEquals(energy, energy(a) + energy(b), 2.0);
        assertEquals(400f, b.vx - a.vx, EPS);
        assertEquals(1, events[0]);
    }

    @Test
    public void obliqueCollisionConservesBothMomentumComponents() {
        World world = world(2);
        world.setRestitution(1f);
        Body a = new Body(400f, 400f, 15f, 0);
        Body b = new Body(415f, 420f, 15f, 0);
        a.vx = 200f;
        a.vy = 150f;
        b.vx = -30f;
        b.vy = -40f;
        float px = a.mass * a.vx + b.mass * b.vx;
        float py = a.mass * a.vy + b.mass * b.vy;
        double energy = energy(a) + energy(b);
        world.addBody(a);
        world.addBody(b);
        world.step(DT);
        assertEquals(px, a.mass * a.vx + b.mass * b.vx, 0.1f);
        assertEquals(py, a.mass * a.vy + b.mass * b.vy, 0.1f);
        assertEquals(energy, energy(a) + energy(b), 3.0);
    }

    @Test
    public void coincidentCentersSeparateWithFiniteState() {
        World world = world(2);
        Body a = new Body(400f, 400f, 10f, 0);
        Body b = new Body(400f, 400f, 20f, 0);
        world.addBody(a);
        world.addBody(b);
        world.step(DT);
        assertTrue(Math.hypot(a.x - b.x, a.y - b.y) >= 30f - EPS);
        for (Body body : world.getBodies()) {
            assertTrue(Float.isFinite(body.x) && Float.isFinite(body.y));
            assertTrue(Float.isFinite(body.vx) && Float.isFinite(body.vy));
        }
    }

    @Test
    public void separatingOverlapDoesNotReceiveAnotherImpulse() {
        World world = world(2);
        Body a = new Body(400f, 400f, 20f, 0);
        Body b = new Body(430f, 400f, 20f, 0);
        a.vx = -100f;
        b.vx = 100f;
        world.addBody(a);
        world.addBody(b);
        int[] events = {0};
        world.setCollisionListener((x, y, speed) -> events[0]++);
        world.step(DT);
        assertEquals(-100f, a.vx, EPS);
        assertEquals(100f, b.vx, EPS);
        assertEquals(0, events[0]);
    }

    @Test
    public void heldBodyStaysFixedWhileOtherBodyBounces() {
        World world = world(2);
        world.setRestitution(1f);
        world.setGravity(500f);
        Body held = new Body(400f, 400f, 20f, 0);
        held.held = true;
        Body moving = new Body(365f, 400f, 20f, 0);
        moving.vx = 200f;
        world.addBody(held);
        world.addBody(moving);
        world.addSpring(new Spring(held, 400f, 100f, 100f, 500f, 10f));
        world.step(DT);
        assertEquals(400f, held.x, 0f);
        assertEquals(400f, held.y, 0f);
        assertEquals(0f, held.vy, 0f);
        assertTrue(moving.vx < 0f);
    }

    @Test
    public void twoHeldBodiesAreNotSeparated() {
        World world = world(2);
        Body a = new Body(400f, 400f, 10f, 0);
        Body b = new Body(400f, 400f, 10f, 0);
        a.held = b.held = true;
        world.addBody(a);
        world.addBody(b);
        world.step(DT);
        assertEquals(400f, a.x, 0f);
        assertEquals(400f, b.x, 0f);
    }

    @Test
    public void springPullsWhenStretchedAndPushesWhenCompressed() {
        for (float y : new float[]{250f, 450f}) {
            World world = world(1);
            Body ball = new Body(400f, y, 10f, 0);
            world.addBody(ball);
            world.addSpring(new Spring(ball, 400f, 100f, 200f, 4000f, 0f));
            world.step(DT);
            assertTrue(y < 300f ? ball.vy > 0f : ball.vy < 0f);
        }
    }

    @Test
    public void dampedSpringApproachesRestLengthWithoutGravity() {
        World world = world(1);
        Body ball = new Body(400f, 450f, 10f, 0);
        world.addBody(ball);
        world.addSpring(new Spring(ball, 400f, 100f, 200f, 4000f, 120f));
        for (int i = 0; i < 600; i++) world.step(DT);
        assertEquals(300f, ball.y, 2f);
        assertEquals(0f, ball.vy, 3f);
    }

    @Test
    public void springAtAnchorDoesNotProduceNaN() {
        World world = world(1);
        Body ball = new Body(400f, 400f, 10f, 0);
        world.addBody(ball);
        world.addSpring(new Spring(ball, 400f, 400f, 50f, 1000f, 10f));
        world.step(DT);
        assertTrue(Float.isFinite(ball.x) && Float.isFinite(ball.y));
        assertTrue(Float.isFinite(ball.vx) && Float.isFinite(ball.vy));
    }

    @Test
    public void longFrameIsClampedAndInvalidTimeDoesNothing() {
        World world = world(1);
        Body ball = new Body(400f, 400f, 10f, 0);
        ball.vx = 90f;
        world.addBody(ball);
        world.step(5f);
        assertEquals(403f, ball.x, EPS);
        world.step(0f);
        world.step(-1f);
        world.step(Float.NaN);
        assertEquals(403f, ball.x, EPS);
    }

    @Test
    public void restitutionIsClampedToZeroAndOne() {
        for (float restitution : new float[]{-1f, 2f}) {
            World world = world(1);
            world.setRestitution(restitution);
            Body ball = new Body(100f, 1990f, 10f, 0);
            ball.vy = 200f;
            world.addBody(ball);
            world.step(DT);
            assertEquals(restitution < 0f ? 0f : -200f, ball.vy, EPS);
        }
    }

    @Test
    public void capacityAndTopmostHitRespectInsertionOrder() {
        World world = world(2);
        Body bottom = new Body(400f, 400f, 20f, 0);
        Body top = new Body(410f, 400f, 20f, 0);
        assertTrue(world.addBody(bottom));
        assertTrue(world.addBody(top));
        assertFalse(world.addBody(new Body(500f, 500f, 20f, 0)));
        assertSame(top, world.findBodyAt(405f, 400f));
        assertSame(bottom, world.findBodyAt(380f, 400f));
        assertNull(world.findBodyAt(1000f, 1000f));
    }

    @Test
    public void returnedListsAreReadOnlyAndClearRemovesSpringsToo() {
        World world = world(2);
        Body ball = new Body(400f, 400f, 10f, 0);
        Spring spring = new Spring(ball, 400f, 100f, 200f, 4000f, 100f);
        world.addBody(ball);
        world.addSpring(spring);
        List<Body> bodies = world.getBodies();
        List<Spring> springs = world.getSprings();
        assertThrows(UnsupportedOperationException.class, () -> bodies.add(ball));
        assertThrows(UnsupportedOperationException.class, springs::clear);
        world.clear();
        assertTrue(bodies.isEmpty());
        assertTrue(springs.isEmpty());
        assertTrue(world.addBody(ball));
    }

    @Test
    public void resizingSmallerThanBallKeepsFiniteCenteredPosition() {
        World world = world(1);
        Body ball = new Body(400f, 400f, 20f, 0);
        ball.vx = 200f;
        world.addBody(ball);
        world.setBounds(10f, 10f);
        world.step(DT);
        assertEquals(5f, ball.x, EPS);
        assertEquals(5f, ball.y, EPS);
        assertEquals(0f, ball.vx, EPS);
        assertEquals(0f, ball.vy, EPS);
    }

    private static double energy(Body body) {
        return 0.5 * body.mass * (body.vx * body.vx + body.vy * body.vy);
    }
}
