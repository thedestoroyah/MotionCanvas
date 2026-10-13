package vn.edu.tdtu.motioncanvas.drawing;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/** Lịch sử thao tác: undo chuyển phần tử cuối sang ngăn redo; thao tác mới xóa ngăn redo. */
public final class UndoStack<T> {
    private final List<T> done = new ArrayList<>();
    private final Deque<T> undone = new ArrayDeque<>();

    public void push(T item) {
        done.add(item);
        undone.clear();
    }

    public boolean undo() {
        if (done.isEmpty()) return false;
        undone.push(done.remove(done.size() - 1));
        return true;
    }

    public boolean redo() {
        if (undone.isEmpty()) return false;
        done.add(undone.pop());
        return true;
    }

    public List<T> items() {
        return Collections.unmodifiableList(done);
    }
}
