package com.innowise.userservice.controller;

import com.innowise.userservice.dto.CardCreateRequest;
import com.innowise.userservice.entity.Card;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.repository.CardRepository;
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
class CardControllerTest extends AbstractIntegrationTest {

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void clearCardRepository() {
        cardRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("Create card")
    class CreateTests {
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should create card and return response when valid data provided")
        void givenValidData_whenCreate_thenSavesCardAndReturnsCardResponse() throws Exception {
            // Given
            var newUser = createTestUser();
            var userId = userRepository.save(newUser).getId();

            var request = new CardCreateRequest(
                    userId,
                    "TEST_NUMBER",
                    "TEST_HOLDER",
                    LocalDate.now().plusDays(1)
            );

            // When, Then
            mockMvc.perform(post("/api/v1/cards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpectAll(
                            jsonPath("$.success", is(true)),
                            jsonPath("$.data.number", is(request.number())),
                            jsonPath("$.data.id").exists()
                    );

            var card = cardRepository.findAll().get(0);
            assertNotNull(card);
            assertEquals(request.userId(), card.getUser().getId());
            assertEquals(request.number(), card.getNumber());
            assertEquals(request.holder(), card.getHolder());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return Conflict when creating card with existing number")
        void givenExistingCardNumber_whenCreate_thenReturnsConflict() throws Exception {
            // Given
            var newUser = createTestUser();
            var userId = userRepository.save(newUser).getId();
            var newCard = new Card(
                    null,
                    newUser,
                    "TEST_NUMBER",
                    "TEST_HOLDER",
                    LocalDate.now().plusDays(1)
            );
            cardRepository.save(newCard);

            var request = new CardCreateRequest(
                    userId,
                    "TEST_NUMBER",
                    "TEST_HOLDER",
                    LocalDate.now().plusDays(1)
            );

            // When, Then
            mockMvc.perform(post("/api/v1/cards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.data").doesNotExist());

            var cards = cardRepository.findAll();
            assertEquals(1, cards.size());
        }
    }

    @Nested
    @DisplayName("Get card by id")
    class GetByIdTests {
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return card response when getting existing card by id")
        void givenExistingId_whenGetById_thenReturnsCardResponse() throws Exception {
            // Given
            var newUser = createTestUser();
            userRepository.save(newUser);

            var newCard = new Card(
                    null,
                    newUser,
                    "TEST_NUMBER",
                    "TEST_HOLDER",
                    LocalDate.now().plusDays(1)
            );
            var id = cardRepository.save(newCard).getId();

            // When, Then
            mockMvc.perform(get("/api/v1/cards/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.id", is(id.toString())))
                    .andExpect(jsonPath("$.data.number", is(newCard.getNumber())))
                    .andExpect(jsonPath("$.data.holder", is(newCard.getHolder())));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return not found when getting non-existing card by id")
        void givenNotExistingId_whenGetById_thenReturnsNotFound() throws Exception {
            // Given
            var newUser = createTestUser();
            userRepository.save(newUser);

            var newCard = new Card(
                    null,
                    newUser,
                    "TEST_NUMBER",
                    "TEST_HOLDER",
                    LocalDate.now().plusDays(1)
            );
            cardRepository.save(newCard);

            // When, Then
            mockMvc.perform(get("/api/v1/cards/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }

    @Nested
    @DisplayName("Get all cards (paged)")
    class GetAllTests {
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return page of cards when getting all cards")
        void givenFewerPagesThanPageSize_whenGetAllCards_thenReturnsPageOfCards() throws Exception {
            // Given
            var newUser = createTestUser();
            userRepository.save(newUser);

            for (int i = 1; i <= 2; i++) {
                cardRepository.save(new Card(
                        null,
                        newUser,
                        "TEST_NUMBER" + i,
                        "TEST_HOLDER" + i,
                        LocalDate.now().plusDays(1)
                ));
            }

            // When, Then
            mockMvc.perform(get("/api/v1/cards"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.totalElements", is(2)))
                    .andExpect(jsonPath("$.data.totalPages", is(1)))
                    .andExpect(jsonPath("$.data.content", hasSize(2)));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return empty page when no cards exist")
        void givenNoCards_whenGetAllCards_thenReturnsEmptyPage() throws Exception {
            // When, Then
            mockMvc.perform(get("/api/v1/cards"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.totalPages", is(0)))
                    .andExpect(jsonPath("$.data.content", hasSize(0)));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return paginated results when cards count more than page size")
        void givenMoreCardsThanPageSize_whenGetAllCards_thenReturnsPaginatedResult() throws Exception {
            // Given
            var newUser = createTestUser();
            userRepository.save(newUser);

            for (int i = 1; i <= 5; i++) {
                cardRepository.save(new Card(
                        null,
                        newUser,
                        "TEST_NUMBER" + i,
                        "TEST_HOLDER" + i,
                        LocalDate.now().plusDays(1)
                ));
            }

            // When, Then
            mockMvc.perform(get("/api/v1/cards?page=0&size=2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.totalElements", is(5)))
                    .andExpect(jsonPath("$.data.totalPages", is(3)))
                    .andExpect(jsonPath("$.data.content", hasSize(2)));
        }
    }

    @Nested
    @DisplayName("Delete card")
    class DeleteTests {
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should delete card when card exists")
        void givenExistingCard_whenDelete_thenDeletesCard() throws Exception {
            // Given
            var newUser = createTestUser();
            userRepository.save(newUser);

            var newCard = new Card(
                    null,
                    newUser,
                    "TEST_NUMBER",
                    "TEST_HOLDER",
                    LocalDate.now().plusDays(1)
            );
            var id = cardRepository.save(newCard).getId();

            // When, Then
            mockMvc.perform(delete("/api/v1/cards/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data").doesNotExist());

            assertFalse(cardRepository.existsById(id));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return not found when deleting not existing card")
        void givenNotExistingCard_whenDelete_thenReturnsNotFound() throws Exception {
            // Given
            var newUser = createTestUser();
            userRepository.save(newUser);

            var newCard = new Card(
                    null,
                    newUser,
                    "TEST_NUMBER",
                    "TEST_HOLDER",
                    LocalDate.now().plusDays(1)
            );
            cardRepository.save(newCard);

            // When, Then
            mockMvc.perform(delete("/api/v1/cards/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.data").doesNotExist());

            assertEquals(1, cardRepository.findAll().size());
        }
    }

    private User createTestUser() {
        var user = new User();
        user.setId(UUID.randomUUID());
        user.setName("TEST_NAME");
        user.setSurname("TEST_SURNAME");
        user.setBirthDate(LocalDate.now().minusDays(1));
        user.setEmail("TEST@EMAIL");
        return user;
    }
}
