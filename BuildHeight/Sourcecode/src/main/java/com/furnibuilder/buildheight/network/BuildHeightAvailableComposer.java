package com.furnibuilder.buildheight.network;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;

public class BuildHeightAvailableComposer extends MessageComposer {

    private final boolean enabled;
    private final int minHeight;
    private final int maxHeight;

    public BuildHeightAvailableComposer(boolean enabled, int minHeight, int maxHeight) {
        this.enabled = enabled;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(BuildHeightProtocol.BUILD_HEIGHT_AVAILABLE);
        this.response.appendBoolean(this.enabled);
        this.response.appendInt(this.minHeight);
        this.response.appendInt(this.maxHeight);
        return this.response;
    }
}
