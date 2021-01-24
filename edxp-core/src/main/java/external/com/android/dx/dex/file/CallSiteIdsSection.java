/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package external.com.android.dx.dex.file;

import external.com.android.dx.rop.cst.Constant;
import external.com.android.dx.rop.cst.CstCallSite;
import external.com.android.dx.rop.cst.CstCallSiteRef;
import java.util.Collection;
import java.util.TreeMap;

/**
 * A section in the DEX file for call site identifiers.
 */
public final class CallSiteIdsSection extends UniformItemSection {

    /** A map from call site references to their DEX file identifier. */
    private final TreeMap<CstCallSiteRef, CallSiteIdItem> callSiteIds = new TreeMap<>();

    /** A map from call site instances to their DEX file item. */
    private final TreeMap<CstCallSite, CallSiteItem> callSites = new TreeMap<>();

    /**
     * Constructs an instance.
     *
     * @param dexFile {@code non-null;} file that this instance is part of
     */
    public CallSiteIdsSection(DexFile dexFile) {
        super("call_site_ids", dexFile, 4);
    }

    /** {@inheritDoc} */
    @Override
    public IndexedItem get(Constant cst) {
        if (cst == null) {
            throw new NullPointerException("cst == null");
        }
        throwIfNotPrepared();

        IndexedItem result = callSiteIds.get((CstCallSiteRef) cst);
        if (result == null) {
            throw new IllegalArgumentException("not found");
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    protected void orderItems() {
        int index = 0;
        for (CallSiteIdItem callSiteId : callSiteIds.values()) {
            callSiteId.setIndex(index++);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Collection<? extends Item> items() {
        return callSiteIds.values();
    }

    /**
     * Interns a call site into this instance.
     *
     * This method is synchronized as it is called during class file translation which runs
     * concurrently on a per class basis.
     *
     * @param cstRef
     */
    public synchronized void intern(CstCallSiteRef cstRef) {
        if (cstRef == null) {
            throw new NullPointerException("cstRef");
        }

        throwIfPrepared();

        CallSiteIdItem result = callSiteIds.get(cstRef);
        if (result == null) {
            result = new CallSiteIdItem(cstRef);
            callSiteIds.put(cstRef, result);
        }
    }

    /**
     * Adds an association between call site constant and its DEX file representation.
     *
     * This method is not synchronized as it is called during DEX file writing which happens
     * concurrently on a per DEX file basis and this information per DEX file.
     *
     * @param callSite {@code non-null;} a constant call site
     * @param callSiteItem {@code non-null;} a call site item as represented in a DEX file
     */
    void addCallSiteItem(CstCallSite callSite, CallSiteItem callSiteItem) {
        if (callSite == null) {
            throw new NullPointerException("callSite == null");
        }
        if (callSiteItem == null) {
            throw new NullPointerException("callSiteItem == null");
        }
        callSites.put(callSite, callSiteItem);
    }

    /**
     * Gets the DEX file representation of a call site associated with a call site constant.
     *
     * This method is not synchronized as it is called during DEX file writing which happens
     * concurrently on a per DEX file basis and this information per DEX file.
     *
     * @param callSite {@code non-null;} a constant call site
     * @return {@code non-null;} a call site item as represented in a DEX file
     */
    CallSiteItem getCallSiteItem(CstCallSite callSite) {
        if (callSite == null) {
            throw new NullPointerException("callSite == null");
        }
        return callSites.get(callSite);
    }
}
