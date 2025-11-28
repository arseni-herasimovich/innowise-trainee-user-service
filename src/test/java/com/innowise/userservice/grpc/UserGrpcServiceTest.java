package com.innowise.userservice.grpc;

import com.innowise.userservice.exception.UserAlreadyExistsException;
import com.innowise.userservice.generated.User;
import com.innowise.userservice.dto.UserCreateRequest;
import com.innowise.userservice.dto.UserResponse;
import com.innowise.userservice.mapper.UserGrpcMapper;
import com.innowise.userservice.service.UserService;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserGrpcServiceTest {
    @Mock
    private UserService userService;

    @Mock
    private UserGrpcMapper userGrpcMapper;

    @Mock
    private StreamObserver<User.UserResponse> responseObserver;

    @InjectMocks
    private UserGrpcService authGrpcService;

    @Test
    @DisplayName("Should create user")
    void givenUserCreateRequest_whenCreateUser_thenReturnsUserCreatedResponse() {
        // Given
        var userId = UUID.randomUUID();

        var request = new UserCreateRequest(
                userId,
                "TEST_NAME",
                "TEST_SURNAME",
                LocalDate.now().minusDays(1),
                "TEST@EMAIL"
        );

        var response = new UserResponse(
                userId,
                "TEST_NAME",
                "TEST_SURNAME",
                LocalDate.now().minusDays(1),
                "TEST@EMAIL",
                null
        );

        var grpcRequest = User.UserCreateRequest.newBuilder()
                .setId(userId.toString())
                .build();

        var grpcResponse = User.UserResponse.newBuilder()
                .setId(userId.toString())
                .build();

        // When
        when(userGrpcMapper.toRequest(grpcRequest)).thenReturn(request);
        when(userService.create(request)).thenReturn(response);
        when(userGrpcMapper.toResponse(response)).thenReturn(grpcResponse);

        authGrpcService.createUser(grpcRequest, responseObserver);

        // Then
        verify(userGrpcMapper, times(1)).toRequest(grpcRequest);
        verify(userService, times(1)).create(request);
        verify(responseObserver, times(1)).onNext(grpcResponse);
        verify(responseObserver, times(1)).onCompleted();
    }

    @Test
    @DisplayName("Should return already exists when user already exists")
    void givenExistingUser_whenCreateUser_thenReturnsStatusAlreadyExists() {
        // Given
        var userId = UUID.randomUUID();

        var request = new UserCreateRequest(
                userId,
                "TEST_NAME",
                "TEST_SURNAME",
                LocalDate.now().minusDays(1),
                "TEST@EMAIL"
        );

        var grpcRequest = User.UserCreateRequest.newBuilder()
                .setId(userId.toString())
                .build();

        // When
        when(userGrpcMapper.toRequest(grpcRequest)).thenReturn(request);
        when(userService.create(request)).thenThrow(UserAlreadyExistsException.class);

        authGrpcService.createUser(grpcRequest, responseObserver);

        // Then
        verify(userGrpcMapper, times(1)).toRequest(grpcRequest);
        verify(userService, times(1)).create(request);
        verify(responseObserver, times(1)).onError(any());
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
    }

    @Test
    @DisplayName("Should return error when exception occurred")
    void givenException_whenCreateUser_thenReturnsStatusUnknown() {
        // Given
        var userId = UUID.randomUUID();

        var request = new UserCreateRequest(
                userId,
                "TEST_NAME",
                "TEST_SURNAME",
                LocalDate.now().minusDays(1),
                "TEST@EMAIL"
        );

        var grpcRequest = User.UserCreateRequest.newBuilder()
                .setId(userId.toString())
                .build();

        // When
        when(userGrpcMapper.toRequest(grpcRequest)).thenReturn(request);
        when(userService.create(request)).thenThrow(RuntimeException.class);

        authGrpcService.createUser(grpcRequest, responseObserver);

        // Then
        verify(userGrpcMapper, times(1)).toRequest(grpcRequest);
        verify(userService, times(1)).create(request);
        verify(responseObserver, times(1)).onError(any());
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
    }
}
