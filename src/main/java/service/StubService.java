package service;

import model.MediaStub;

public interface StubService {

    boolean addNewStub(MediaStub mediaStub);

    boolean editStub(Long mediaStubId);

    boolean deleteStub(Long mediaStubId);

}
