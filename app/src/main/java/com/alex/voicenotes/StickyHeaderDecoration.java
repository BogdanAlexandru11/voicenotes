package com.alex.voicenotes;

import android.graphics.Canvas;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class StickyHeaderDecoration extends RecyclerView.ItemDecoration {

    private final StickyHeaderInterface stickyHeaderInterface;

    public StickyHeaderDecoration(StickyHeaderInterface stickyHeaderInterface) {
        this.stickyHeaderInterface = stickyHeaderInterface;
    }

    @Override
    public void onDrawOver(@NonNull Canvas canvas, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.onDrawOver(canvas, parent, state);

        View topChild = parent.getChildAt(0);
        if (topChild == null) return;

        int topChildPosition = parent.getChildAdapterPosition(topChild);
        if (topChildPosition == RecyclerView.NO_POSITION) return;

        int headerPosition = stickyHeaderInterface.getHeaderPositionForItem(topChildPosition);
        if (headerPosition == RecyclerView.NO_POSITION) return;

        View currentHeader = getHeaderViewForItem(headerPosition, parent);
        fixLayoutSize(parent, currentHeader);

        int contactPoint = currentHeader.getBottom();
        View childInContact = getChildInContact(parent, contactPoint, headerPosition);

        if (childInContact != null && stickyHeaderInterface.isHeader(parent.getChildAdapterPosition(childInContact))) {
            moveHeader(canvas, currentHeader, childInContact);
            return;
        }

        drawHeader(canvas, currentHeader);
    }

    private View getHeaderViewForItem(int headerPosition, RecyclerView parent) {
        View header = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.month_header_item, parent, false);
        stickyHeaderInterface.bindHeaderData(header, headerPosition);
        return header;
    }

    private void drawHeader(Canvas canvas, View header) {
        canvas.save();
        canvas.translate(0, 0);
        header.draw(canvas);
        canvas.restore();
    }

    private void moveHeader(Canvas canvas, View currentHeader, View nextHeader) {
        canvas.save();
        canvas.translate(0, nextHeader.getTop() - currentHeader.getHeight());
        currentHeader.draw(canvas);
        canvas.restore();
    }

    private View getChildInContact(RecyclerView parent, int contactPoint, int currentHeaderPosition) {
        View childInContact = null;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            int position = parent.getChildAdapterPosition(child);
            if (position > currentHeaderPosition && child.getTop() <= contactPoint && child.getBottom() > contactPoint) {
                if (stickyHeaderInterface.isHeader(position)) {
                    childInContact = child;
                    break;
                }
            }
        }
        return childInContact;
    }

    private void fixLayoutSize(ViewGroup parent, View view) {
        int widthSpec = View.MeasureSpec.makeMeasureSpec(parent.getWidth(), View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(parent.getHeight(), View.MeasureSpec.UNSPECIFIED);

        int childWidthSpec = ViewGroup.getChildMeasureSpec(widthSpec,
                parent.getPaddingLeft() + parent.getPaddingRight(), view.getLayoutParams().width);
        int childHeightSpec = ViewGroup.getChildMeasureSpec(heightSpec,
                parent.getPaddingTop() + parent.getPaddingBottom(), view.getLayoutParams().height);

        view.measure(childWidthSpec, childHeightSpec);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
    }

    public interface StickyHeaderInterface {
        boolean isHeader(int itemPosition);
        int getHeaderPositionForItem(int itemPosition);
        void bindHeaderData(View header, int headerPosition);
    }
}
