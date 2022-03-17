package org.lsposed.manager.util;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;

public class EmptyAccessibilityDelegate extends View.AccessibilityDelegate {

    @Override
    public void sendAccessibilityEvent(View host, int eventType) {

    }

    @Override
    public boolean performAccessibilityAction(View host, int action, Bundle args) {
        return true;
    }

    @Override
    public void sendAccessibilityEventUnchecked(View host, AccessibilityEvent event) {

    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
        return true;
    }

    @Override
    public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {

    }

    @Override
    public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {

    }

    @Override
    public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {

    }

    @Override
    public void addExtraDataToAccessibilityNodeInfo(View host, AccessibilityNodeInfo info, String extraDataKey, Bundle arguments) {

    }

    @Override
    public boolean onRequestSendAccessibilityEvent(ViewGroup host, View child, AccessibilityEvent event) {
        return true;
    }

    @Override
    public AccessibilityNodeProvider getAccessibilityNodeProvider(View host) {
        return null;
    }
}
