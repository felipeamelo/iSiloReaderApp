package com.dcco.app.iSilo.state;

public final class SessionState {

    public DocState docState;

    public SessionState() {
    }

    public void close() {
        if (this.docState != null) {
            this.docState.destroy();
            this.docState = null;
        }
    }

    public boolean isPageMode() {
        return (this.docState.flags & 4) == 0;
    }
}
