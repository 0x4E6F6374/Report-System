package cn.reportsystem.core.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.ArrayList;
import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public final class Actions {

    public List<String> onResolve = new ArrayList<String>();
    public List<String> onReject = new ArrayList<String>();

    public List<String> forResolve() {
        return onResolve == null ? new ArrayList<String>() : onResolve;
    }

    public List<String> forReject() {
        return onReject == null ? new ArrayList<String>() : onReject;
    }
}
