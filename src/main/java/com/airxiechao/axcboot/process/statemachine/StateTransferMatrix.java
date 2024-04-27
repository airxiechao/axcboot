package com.airxiechao.axcboot.process.statemachine;

import java.util.ArrayList;
import java.util.List;

public class StateTransferMatrix {

    private List<Path> stm = new ArrayList<>();

    public void addPath(String from, String to){
        stm.add(new Path(from, to));
    }

    public boolean hasPath(String from, String to){
        for(Path path : stm){
            if(path.getFrom().equals(from) && path.getTo().equals(to)){
                return true;
            }
        }

        return false;
    }

    public static class Path{
        private String from;
        private String to;

        public Path(String from, String to) {
            this.from = from;
            this.to = to;
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }
    }
}
