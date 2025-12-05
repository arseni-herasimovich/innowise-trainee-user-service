package com.innowise.userservice.controller;

import com.innowise.userservice.dto.UserCreateRequest;
import com.innowise.userservice.dto.UserUpdateRequest;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void clearUserRepository() {
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("Create user")
    class CreateTests {
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should create user and return response when valid request provided")
        void givenValidRequest_whenCreate_thenSavesToDatabaseAndResponses() throws Exception {
            // Given
            var request = new UserCreateRequest(
                    UUID.randomUUID().toString(),
                    "TEST_NAME",
                    "TEST_SURNAME",
                    LocalDate.now().minusDays(1),
                    "TEST@EMAIL"
            );

            // When, Then
            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpectAll(
                            jsonPath("$.success", is(true)),
                            jsonPath("$.data.email", is(request.email())),
                            jsonPath("$.data.userId", is(request.userId()))
                    );

            var user = userRepository.findUserByEmail(request.email());
            assertTrue(user.isPresent());
            assertNotNull(user.get().getId());
            assertEquals(request.name(), user.get().getName());
            assertEquals(request.surname(), user.get().getSurname());
            assertEquals(request.birthDate(), user.get().getBirthDate());
            assertEquals(request.email(), user.get().getEmail());
            assertEquals(request.userId(), user.get().getUserId());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return conflict when creating user with existing email")
        void givenExistingEmail_whenCreate_thenReturnsConflict() throws Exception {
            // Given
            var newUser = createTestUser();
            userRepository.save(newUser);

            var request = new UserCreateRequest(
                    UUID.randomUUID().toString(),
                    "TEST_NAME",
                    "TEST_SURNAME",
                    LocalDate.now().minusDays(1),
                    "TEST@EMAIL"
            );

            // When, Then
            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.data").doesNotExist());

            var users = userRepository.findAll();
            assertEquals(1, users.size());
            assertEquals("TEST@EMAIL", users.get(0).getEmail());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return conflict when creating user with existing user ID")
        void givenExistingUserId_whenCreate_thenReturnsConflict() throws Exception {
            // Given
            var newUser = createTestUser();
            userRepository.save(newUser);

            var request = new UserCreateRequest(
                    newUser.getUserId(),
                    "TEST_NAME",
                    "TEST_SURNAME",
                    LocalDate.now().minusDays(1),
                    "NEW@EMAIL"
            );

            // When, Then
            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.data").doesNotExist());

            var users = userRepository.findAll();
            assertEquals(1, users.size());
            assertEquals("TEST@EMAIL", users.get(0).getEmail());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return bad request when creating user with not valid date")
        void givenCorruptedDate_whenCreate_thenReturnsBadRequest() throws Exception {
            // Given
            var request = Map.of(
                    "id", UUID.randomUUID().toString(),
                    "name", "TEST_NAME",
                    "surname", "TEST_SURNAME",
                    "birthDate", "NOT VALID DATE",
                    "email", "TEST@EMAIL"
            );

            // When, Then
            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.data").doesNotExist());

            List<User> users = userRepository.findAll();
            assertEquals(0, users.size());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return bad request when creating user with not valid email")
        void givenCorruptedEmail_whenCreate_thenReturnsBadRequest() throws Exception {
            // Given
            var request = new UserCreateRequest(
                    UUID.randomUUID().toString(),
                    "TEST_NAME",
                    "TEST_SURNAME",
                    LocalDate.now().minusDays(1),
                    "TESTEMAIL"
            );

            // When, Then
            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));

            List<User> users = userRepository.findAll();
            assertEquals(0, users.size());
        }
    }

    @Nested
    @DisplayName("Get user by id")
    class GetByIdTests {
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return user response when getting existing user by id")
        void givenExistingId_whenGetById_thenReturnsUserResponse() throws Exception {
            // Given
            var newUser = createTestUser();
            var id = userRepository.save(newUser).getUserId();

            // When, Then..,
            mockMvc.perform(get("/api/v1/users/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.userId", is(id)))
                    .andExpect(jsonPath("$.data.name", is(newUser.getName())))
                    .andExpect(jsonPath("$.data.email", is(newUser.getEmail())))
                    .andExpect(jsonPath("$.data.birthDate", is(newUser.getBirthDate().toString())))
                    .andExpect(jsonPath("$.data.surname", is(newUser.getSurname())));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return not found when getting non-existing user by id")
        void givenNotExistingId_whenGetById_thenReturnsNotFound() throws Exception {
            // Given
            var newUser = createTestUser();
            userRepository.save(newUser);

            // When, Then
            mockMvc.perform(get("/api/v1/users/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }

    @Nested
    @DisplayName("Get user by email")
    class GetByEmailTests {
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return user response when getting existing user by email")
        void givenExistingEmail_whenGetByEmail_thenReturnsUserResponse() throws Exception {
            // Given
            var newUser = createTestUser();
            var email = userRepository.save(newUser).getEmail();

            // When, Then
            mockMvc.perform(get("/api/v1/users/search")
                            .param("email", email))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.email", is(email)));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return not found when getting non-existing user by email")
        void givenNotExistingEmail_whenGetByEmail_thenReturnsNotFound() throws Exception {
            // Given
            var newUser = createTestUser();
            userRepository.save(newUser);

            // When, Then
            mockMvc.perform(get("/api/v1/users/search")
                            .param("email", "NO@EMAIL"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }

    @Nested
    @DisplayName("Get all users (paged)")
    class GetAllTests {
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return page of users when getting all users")
        void givenFewerPagesThanPageSize_whenGetAllUsers_thenReturnsPageOfUsers() throws Exception {
            // Given
            for (int i = 1; i <= 2; i++) {
                var newUser = createUniqueUser(i);
                userRepository.save(newUser);
            }

            // When, Then
            mockMvc.perform(get("/api/v1/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.totalElements", is(2)))
                    .andExpect(jsonPath("$.data.totalPages", is(1)))
                    .andExpect(jsonPath("$.data.content", hasSize(2)));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return empty page when no users exist")
        void givenNoUsers_whenGetAllUsers_thenReturnsEmptyPage() throws Exception {
            // When, Then
            mockMvc.perform(get("/api/v1/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.totalPages", is(0)))
                    .andExpect(jsonPath("$.data.content", hasSize(0)));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return paginated results when users count more than page size")
        void givenMoreUsersThanPageSize_whenGetAllUsers_thenReturnsPaginatedResult() throws Exception {
            // Given
            for (int i = 1; i <= 5; i++) {
                var newUser = createUniqueUser(i);
                userRepository.save(newUser);
            }

            // When, Then
            mockMvc.perform(get("/api/v1/users?page=0&size=2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.totalElements", is(5)))
                    .andExpect(jsonPath("$.data.totalPages", is(3)))
                    .andExpect(jsonPath("$.data.content", hasSize(2)));
        }
    }

    @Nested
    @DisplayName("Update user")
    class UpdateTests {
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should update user when user exists and email is not changing")
        void givenExistingUserWithoutChangingEmail_whenUpdate_thenUpdatesUser() throws Exception {
            // Given
            var newUser = createTestUser();
            var id = userRepository.save(newUser).getUserId();

            var request = new UserUpdateRequest(
                    "TEST_NAME",
                    "TEST_SURNAME",
                    LocalDate.now().minusDays(1),
                    null
            );

            // When, Then
            mockMvc.perform(patch("/api/v1/users/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data").doesNotExist());

            var user = userRepository.findUserByUserId(id).orElseThrow();

            assertEquals(request.name(), user.getName());
            assertEquals(request.surname(), user.getSurname());
            assertNotEquals(request.email(), user.getEmail()); // Email should not change
            assertEquals(request.birthDate(), user.getBirthDate());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should update user when user exists, email is changing and email does not exist")
        void givenExistingUserWithChangingEmailAndEmailNotExists_whenUpdate_thenUpdatesUser() throws Exception {
            // Given
            var newUser = createTestUser();
            var id = userRepository.save(newUser).getUserId();

            var request = new UserUpdateRequest(
                    "TEST_NAME",
                    "TEST_SURNAME",
                    LocalDate.now().minusDays(1),
                    "NOTEXISTINGEMAIL@EMAIL"
            );

            // When, Then
            mockMvc.perform(patch("/api/v1/users/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data").doesNotExist());

            var user = userRepository.findUserByUserId(id).orElseThrow();

            assertEquals(request.name(), user.getName());
            assertEquals(request.surname(), user.getSurname());
            assertEquals(request.email(), user.getEmail()); // Email should change
            assertEquals(request.birthDate(), user.getBirthDate());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should not update user when user exists, email is changing and email exists")
        void givenExistingUserWithChangingEmailAndEmailExists_whenUpdate_thenReturnsConflict() throws Exception {
            // Given
            var existingUser = createUniqueUser(1);
            userRepository.save(existingUser);

            var newUser = createTestUser();
            var id = userRepository.save(newUser).getUserId();

            var request = new UserUpdateRequest(
                    "NEW_NAME",
                    "NEW_SURNAME",
                    LocalDate.now().minusDays(100),
                    existingUser.getEmail()
            );

            // When, Then
            mockMvc.perform(patch("/api/v1/users/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.data").doesNotExist());

            var user = userRepository.findUserByUserId(id).orElseThrow();

            assertNotEquals(request.name(), user.getName());
            assertNotEquals(request.surname(), user.getSurname());
            assertNotEquals(request.email(), user.getEmail());
            assertNotEquals(request.birthDate(), user.getBirthDate());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return Not Found when updating non-existing user")
        void givenNonExistingUser_whenUpdate_thenReturnsNotFound() throws Exception {
            // Given
            var id = UUID.randomUUID();
            var request = new UserUpdateRequest(
                    "TEST_NAME",
                    "TEST_SURNAME",
                    LocalDate.now().minusDays(1),
                    "EXISTING@EMAIL"
            );

            // When, Then
            mockMvc.perform(patch("/api/v1/users/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.data").doesNotExist());

            assertFalse(userRepository.existsById(id));
        }
    }

    @Nested
    @DisplayName("Delete user")
    class DeleteTests {
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should delete user when user exists")
        void givenExistingUser_whenDelete_thenDeletesUser() throws Exception {
            // Given
            var newUser = createTestUser();
            var id = userRepository.save(newUser).getUserId();

            // When, Then
            mockMvc.perform(delete("/api/v1/users/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data").doesNotExist());

            assertFalse(userRepository.existsByUserId(id));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return NOT FOUND when user does not exist")
        void givenNotExistingUser_whenDelete_thenReturnsNotFound() throws Exception {
            // Given
            var newUser = createTestUser();
            var newUserId = userRepository.save(newUser).getId();
            var id = UUID.randomUUID().toString();

            // When, Then
            mockMvc.perform(delete("/api/v1/users/{id}", id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.data").doesNotExist());

            assertTrue(userRepository.existsById(newUserId));
        }
    }

    private User createTestUser() {
        User user = new User();
        user.setName("TEST_NAME");
        user.setSurname("TEST_SURNAME");
        user.setBirthDate(LocalDate.now().minusDays(1));
        user.setEmail("TEST@EMAIL");
        user.setUserId(UUID.randomUUID().toString());
        return user;
    }

    private User createUniqueUser(int i) {
        User user = new User();
        user.setName("TEST_NAME" + i);
        user.setSurname("TEST_SURNAME" + i);
        user.setBirthDate(LocalDate.now().minusDays(1));
        user.setEmail("TEST@EMAIL" + i);
        user.setUserId(UUID.randomUUID().toString());
        return user;
    }
}