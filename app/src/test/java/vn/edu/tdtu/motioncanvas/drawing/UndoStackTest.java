package vn.edu.tdtu.motioncanvas.drawing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class UndoStackTest {
    @Test
    public void undoRemovesLastAndRedoRestoresIt() {
        UndoStack<String> stack = new UndoStack<>();
        stack.push("a");
        stack.push("b");

        assertTrue(stack.undo());
        assertEquals(Collections.singletonList("a"), stack.items());

        assertTrue(stack.redo());
        assertEquals(Arrays.asList("a", "b"), stack.items());
    }

    @Test
    public void pushAfterUndoClearsRedo() {
        UndoStack<String> stack = new UndoStack<>();
        stack.push("a");
        stack.push("b");
        stack.undo();

        stack.push("c");

        assertFalse(stack.redo());
        assertEquals(Arrays.asList("a", "c"), stack.items());
    }

    @Test
    public void undoAndRedoOnEmptyReturnFalse() {
        UndoStack<String> stack = new UndoStack<>();
        assertFalse(stack.undo());
        assertFalse(stack.redo());
        assertTrue(stack.items().isEmpty());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void itemsIsReadOnly() {
        UndoStack<String> stack = new UndoStack<>();
        stack.items().add("x");
    }
}
