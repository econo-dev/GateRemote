package com.gal.gateremote;

import static com.gal.gateremote.Gate.State.CLOSE;
import static com.gal.gateremote.Gate.State.OPEN;
import static com.gal.gateremote.Gate.State.STOP;

class Gate {

    private int i=0;
    private State state;

    public enum State {
        OPEN,
        STOP,
        CLOSE
    }
    public Gate(){};

    public Gate(State state) {
        this.state = state;
    }

    public void setOpen(State OPEN) {
        this.state = OPEN;
    }

    public void setStop(State STOP) {
        this.state = STOP;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public void nextState() {

        switch (this.getState()){
            case OPEN:
                i=1;
                this.state = State.STOP;
                break;
            case STOP:
                this.state = i==1 ? CLOSE : OPEN;
                break;
            case CLOSE:
                i=2;
                this.state = STOP;
                break;
        }
    }

    public void setClose(State CLOSE) {
        this.state = CLOSE;
    }
}
