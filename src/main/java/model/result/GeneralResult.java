package model.result;

import java.util.List;

public class GeneralResult<T> {
    private Response status;
    private List<T> value;
    private String error;

    public Response getStatus() {
        return status;
    }

    public void setStatus(Response status) {
        this.status = status;
    }

    public List<T> getValue() {
        return value;
    }

    public void setValue(List<T> value) {
        this.value = value;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
