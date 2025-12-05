package com.innowise.userservice.grpc;

import com.innowise.userservice.generated.User;
import com.innowise.userservice.generated.UserServiceGrpc;
import com.innowise.userservice.exception.UserAlreadyExistsException;
import com.innowise.userservice.mapper.UserGrpcMapper;
import com.innowise.userservice.service.UserService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {
    private final UserService userService;
    private final UserGrpcMapper userGrpcMapper;

    @Override
    public void createUser(User.UserCreateRequest request, StreamObserver<User.UserResponse> responseObserver) {
        log.debug("Received request to create user with id: {}", request.getUserId());
        try {
            var response = userService.create(
                    userGrpcMapper.toRequest(request)
            );
            log.debug("User with id {} created successfully", response.userId());
            responseObserver.onNext(userGrpcMapper.toResponse(response));
            responseObserver.onCompleted();
        } catch (UserAlreadyExistsException e) {
            log.debug("User with id {} already exists", request.getUserId());
            var status = Status.ALREADY_EXISTS.withDescription(e.getMessage()).asRuntimeException();
            responseObserver.onError(status);
        }
        catch (Exception e) {
            log.error("Error while creating user", e);
            var status = Status.UNKNOWN.withDescription("Internal server error").asRuntimeException();
            responseObserver.onError(status);
        }
    }
}
