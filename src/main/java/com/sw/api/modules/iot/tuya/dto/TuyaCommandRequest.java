package com.sw.api.modules.iot.tuya.dto;

import java.util.List;

public record TuyaCommandRequest(List<Command> commands) {
    public record Command(String code, Object value) {
    }

    public static TuyaCommandRequest switchCommand(boolean on) {
        return new TuyaCommandRequest(List.of(new Command("switch_1", on)));
    }
}
