package net.petrikainulainen.spring.jooq.todo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.petrikainulainen.spring.jooq.WebTestConstants;
import net.petrikainulainen.spring.jooq.WebTestUtil;
import net.petrikainulainen.spring.jooq.common.TestDateUtil;
import net.petrikainulainen.spring.jooq.config.WebUnitTestContext;
import net.petrikainulainen.spring.jooq.todo.dto.TodoDTO;
import net.petrikainulainen.spring.jooq.todo.dto.TodoDTOBuilder;
import net.petrikainulainen.spring.jooq.todo.exception.TodoNotFoundException;
import net.petrikainulainen.spring.jooq.todo.service.TodoCrudService;
import net.petrikainulainen.spring.jooq.todo.service.TodoSearchService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.Arrays;

import static net.petrikainulainen.spring.jooq.todo.dto.TodoDTOAssert.assertThatTodoDTO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Petri Kainulainen
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {WebUnitTestContext.class})
@WebAppConfiguration
public class TodoControllerTest {

    private static final String CREATION_TIME = TestDateUtil.CURRENT_TIMESTAMP;
    private static final String DESCRIPTION = "description";
    private static final Long ID = 1L;
    private static final String MODIFICATION_TIME = TestDateUtil.CURRENT_TIMESTAMP;
    private static final String TITLE = "title";
    private static final String SEARCH_TERM = "IT";

    private static final int PAGE_NUMBER = 0;
    private static final String PAGE_NUMBER_STRING = PAGE_NUMBER + "";
    private static final int PAGE_SIZE = 10;
    private static final String PAGE_SIZE_STRING = PAGE_SIZE + "";
    private static final String SORT_FIELD = "id";
    private static final String SORT_ORDER = "ASC";

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TodoCrudService todoCrudServiceMock;

    @Autowired
    private TodoSearchService todoSearchServiceMock;

    @Autowired
    private WebApplicationContext webAppContext;

    @Before
    public void setUp() {
        Mockito.reset(todoCrudServiceMock, todoSearchServiceMock);

        mockMvc = MockMvcBuilders.webAppContextSetup(webAppContext).build();
    }

    @Test
    public void add_EmptyTodoEntry_ShouldReturnValidationErrorAboutMissingTitleAsJsonDocument() throws Exception {
        TodoDTO addedTodoEntry = new TodoDTO();

        mockMvc.perform(post("/api/todo")
                        .contentType(WebTestConstants.APPLICATION_JSON_UTF8)
                        .content(objectMapper.writeValueAsBytes(addedTodoEntry))
        )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(WebTestConstants.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.name())))
                .andExpect(jsonPath("$.code", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.message", not(isEmptyOrNullString())))
                .andExpect(jsonPath("$.validationErrors", hasSize(1)))
                .andExpect(jsonPath("$.validationErrors[?(@.field=='title')].errorCode", contains(WebTestConstants.ERROR_CODE_NOT_EMPTY)))
                .andExpect(jsonPath("$.validationErrors[?(@.field=='title')].errorMessage", not(isEmptyOrNullString())));

        verifyZeroInteractions(todoCrudServiceMock, todoSearchServiceMock);
    }

    @Test
    public void add_TitleAndDescriptionAreTooLong_ShouldReturnValidationErrorsAboutTitleAndDescriptionAsJsonDocument() throws Exception {
        String tooLongTitle = WebTestUtil.createStringWithLength(101);
        String tooLongDescription = WebTestUtil.createStringWithLength(501);

        TodoDTO addedTodoEntry = new TodoDTOBuilder()
                .description(tooLongDescription)
                .title(tooLongTitle)
                .build();

        mockMvc.perform(post("/api/todo")
                        .contentType(WebTestConstants.APPLICATION_JSON_UTF8)
                        .content(objectMapper.writeValueAsBytes(addedTodoEntry))
        )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(WebTestConstants.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.name())))
                .andExpect(jsonPath("$.code", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.message", not(isEmptyOrNullString())))
                .andExpect(jsonPath("$.validationErrors", hasSize(2)))
                .andExpect(jsonPath("$.validationErrors[?(@.field=='description')].errorCode", contains(WebTestConstants.ERROR_CODE_LENGTH)))
                .andExpect(jsonPath("$.validationErrors[?(@.field=='description')].errorMessage", not(isEmptyOrNullString())))
                .andExpect(jsonPath("$.validationErrors[?(@.field=='title')].errorCode", contains(WebTestConstants.ERROR_CODE_LENGTH)))
                .andExpect(jsonPath("$.validationErrors[?(@.field=='title')].errorMessage", not(isEmptyOrNullString())));

        verifyZeroInteractions(todoCrudServiceMock, todoSearchServiceMock);
    }

    @Test
    public void add_TodoEntryAdded_ShouldReturnAddedToEntryAsJsonDocument() throws Exception {
        TodoDTO addedTodoEntry = new TodoDTOBuilder()
                .description(DESCRIPTION)
                .title(TITLE)
                .build();

        TodoDTO returnedTodoEntry = new TodoDTOBuilder()
                .creationTime(CREATION_TIME)
                .description(DESCRIPTION)
                .id(ID)
                .modificationTime(MODIFICATION_TIME)
                .title(TITLE)
                .build();

        when(todoCrudServiceMock.add(isA(TodoDTO.class))).thenReturn(returnedTodoEntry);

        mockMvc.perform(post("/api/todo")
                        .contentType(WebTestConstants.APPLICATION_JSON_UTF8)
                        .content(objectMapper.writeValueAsBytes(addedTodoEntry))
        )
                .andExpect(status().isCreated())
                .andExpect(content().contentType(WebTestConstants.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.id", is(ID.intValue())))
                .andExpect(jsonPath("$.creationTime", is(CREATION_TIME)))
                .andExpect(jsonPath("$.description", is(DESCRIPTION)))
                .andExpect(jsonPath("$.modificationTime", is(MODIFICATION_TIME)))
                .andExpect(jsonPath("$.title", is(TITLE)));

        ArgumentCaptor<TodoDTO> serviceMethodArgument = ArgumentCaptor.forClass(TodoDTO.class);

        verify(todoCrudServiceMock, times(1)).add(serviceMethodArgument.capture());
        verifyNoMoreInteractions(todoCrudServiceMock);
        verifyZeroInteractions(todoSearchServiceMock);

        TodoDTO serviceMethodArgumentValue = serviceMethodArgument.getValue();
        assertThatTodoDTO(serviceMethodArgumentValue)
                .hasNoId()
                .hasNoCreationTime()
                .hasDescription(DESCRIPTION)
                .hasNoModificationTime()
                .hasTitle(TITLE);
    }

    @Test
    public void delete_TodoEntryNotFound_ShouldReturnHttpStatusCodeNotFound() throws Exception {
        when(todoCrudServiceMock.delete(ID)).thenThrow(new TodoNotFoundException(""));

        mockMvc.perform(delete("/api/todo/{id}", ID))
                .andExpect(status().isNotFound());

        verify(todoCrudServiceMock, times(1)).delete(ID);
        verifyNoMoreInteractions(todoCrudServiceMock);
        verifyZeroInteractions(todoSearchServiceMock);
    }

    @Test
    public void delete_TodoEntryFound_ShouldReturnDeletedTodoEntryAsJsonDocument() throws Exception {
        TodoDTO deletedTodoEntry = new TodoDTOBuilder()
                .creationTime(CREATION_TIME)
                .description(DESCRIPTION)
                .id(ID)
                .modificationTime(MODIFICATION_TIME)
                .title(TITLE)
                .build();

        when(todoCrudServiceMock.delete(ID)).thenReturn(deletedTodoEntry);

        mockMvc.perform(delete("/api/todo/{id}", ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(WebTestConstants.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.id", is(ID.intValue())))
                .andExpect(jsonPath("$.creationTime", is(CREATION_TIME)))
                .andExpect(jsonPath("$.description", is(DESCRIPTION)))
                .andExpect(jsonPath("$.modificationTime", is(MODIFICATION_TIME)))
                .andExpect(jsonPath("$.title", is(TITLE)));

        verify(todoCrudServiceMock, times(1)).delete(ID);
        verifyNoMoreInteractions(todoCrudServiceMock);
        verifyZeroInteractions(todoSearchServiceMock);
    }

    @Test
    public void findAll_NoTodoEntriesFound_ShouldReturnEmptyListAsJSonDocument() throws Exception {
        when(todoCrudServiceMock.findAll()).thenReturn(new ArrayList<TodoDTO>());

        mockMvc.perform(get("/api/todo"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(WebTestConstants.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$", hasSize(0)));

        verify(todoCrudServiceMock, times(1)).findAll();
        verifyNoMoreInteractions(todoCrudServiceMock);
        verifyZeroInteractions(todoSearchServiceMock);
    }

    @Test
    public void findAll_OneTodoEntryFound_ShouldReturnTodoEntriesAsJsonDocument() throws Exception {
        TodoDTO foundTodoEntry = new TodoDTOBuilder()
                .creationTime(CREATION_TIME)
                .description(DESCRIPTION)
                .id(ID)
                .modificationTime(MODIFICATION_TIME)
                .title(TITLE)
                .build();

        when(todoCrudServiceMock.findAll()).thenReturn(Arrays.asList(foundTodoEntry));

        mockMvc.perform(get("/api/todo"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(WebTestConstants.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(ID.intValue())))
                .andExpect(jsonPath("$[0].creationTime", is(CREATION_TIME)))
                .andExpect(jsonPath("$[0].description", is(DESCRIPTION)))
                .andExpect(jsonPath("$[0].modificationTime", is(MODIFICATION_TIME)))
                .andExpect(jsonPath("$[0].title", is(TITLE)));

        verify(todoCrudServiceMock, times(1)).findAll();
        verifyNoMoreInteractions(todoCrudServiceMock);
        verifyZeroInteractions(todoSearchServiceMock);
    }

    @Test
    public void findById_TodoEntryFound_ShouldReturnTodoEntryAsJsonDocument() throws Exception {
        TodoDTO foundTodoEntry = new TodoDTOBuilder()
                .creationTime(CREATION_TIME)
                .description(DESCRIPTION)
                .id(ID)
                .modificationTime(MODIFICATION_TIME)
                .title(TITLE)
                .build();

        when(todoCrudServiceMock.findById(ID)).thenReturn(foundTodoEntry);

        mockMvc.perform(get("/api/todo/{id}", ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(WebTestConstants.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.id", is(ID.intValue())))
                .andExpect(jsonPath("$.creationTime", is(CREATION_TIME)))
                .andExpect(jsonPath("$.description", is(DESCRIPTION)))
                .andExpect(jsonPath("$.modificationTime", is(MODIFICATION_TIME)))
                .andExpect(jsonPath("$.title", is(TITLE)));

        verify(todoCrudServiceMock, times(1)).findById(ID);
        verifyNoMoreInteractions(todoCrudServiceMock);
        verifyZeroInteractions(todoSearchServiceMock);
    }

    @Test
    public void findById_TodoEntryNotFound_ShouldReturnHttpStatusCodeNotFound() throws Exception {
        when(todoCrudServiceMock.findById(ID)).thenThrow(new TodoNotFoundException(""));

        mockMvc.perform(get("/api/todo/{id}", ID))
                .andExpect(status().isNotFound());

        verify(todoCrudServiceMock, times(1)).findById(ID);
        verifyNoMoreInteractions(todoCrudServiceMock);
        verifyZeroInteractions(todoSearchServiceMock);
    }

    @Test
    public void findBySearchTerm_NoTodoEntriesFound_ShouldReturnEmptyListAsJsonDocument() throws Exception {
        when(todoSearchServiceMock.findBySearchTerm(eq(SEARCH_TERM), isA(Pageable.class))).thenReturn(new ArrayList<TodoDTO>());

        mockMvc.perform(get("/api/todo/search")
                .param(WebTestConstants.REQUEST_PARAM_SEARCH_TERM, SEARCH_TERM)
                .param(WebTestConstants.REQUEST_PARAM_PAGE_NUMBER, PAGE_NUMBER_STRING)
                .param(WebTestConstants.REQUEST_PARAM_PAGE_SIZE, PAGE_SIZE_STRING)
                .param(WebTestConstants.REQUEST_PARAM_SORT_FIELD, SORT_FIELD)
                .param(WebTestConstants.REQUEST_PARAM_SORT_ORDER, SORT_ORDER)

        )
                .andExpect(status().isOk())
                .andExpect(content().contentType(WebTestConstants.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$", hasSize(0)));

        ArgumentCaptor<Pageable> pageableArgument = ArgumentCaptor.forClass(Pageable.class);

        verify(todoSearchServiceMock, times(1)).findBySearchTerm(eq(SEARCH_TERM), pageableArgument.capture());
        verifyNoMoreInteractions(todoSearchServiceMock);
        verifyZeroInteractions(todoCrudServiceMock);

        Pageable pageSpecification = pageableArgument.getValue();

        assertThat(pageSpecification.getPageNumber()).isEqualTo(PAGE_NUMBER);
        assertThat(pageSpecification.getPageSize()).isEqualTo(PAGE_SIZE);

        Sort sortSpecification = pageSpecification.getSort();
        assertThat(sortSpecification.getOrderFor(SORT_FIELD).getDirection()).isEqualTo(Sort.Direction.fromString(SORT_ORDER));
    }

    @Test
    public void findBySearchTerm_OneTodoEntryFound_ShouldReturnTodoEntriesAsJsonDocument() throws Exception {
        TodoDTO foundTodoEntry = new TodoDTOBuilder()
                .creationTime(CREATION_TIME)
                .description(DESCRIPTION)
                .id(ID)
                .modificationTime(MODIFICATION_TIME)
                .title(TITLE)
                .build();

        when(todoSearchServiceMock.findBySearchTerm(eq(SEARCH_TERM), isA(Pageable.class))).thenReturn(Arrays.asList(foundTodoEntry));

        mockMvc.perform(get("/api/todo/search")
                .param(WebTestConstants.REQUEST_PARAM_SEARCH_TERM, SEARCH_TERM)
                .param(WebTestConstants.REQUEST_PARAM_PAGE_NUMBER, PAGE_NUMBER_STRING)
                .param(WebTestConstants.REQUEST_PARAM_PAGE_SIZE, PAGE_SIZE_STRING)
                .param(WebTestConstants.REQUEST_PARAM_SORT_FIELD, SORT_FIELD)
                .param(WebTestConstants.REQUEST_PARAM_SORT_ORDER, SORT_ORDER)
        )
                .andExpect(status().isOk())
                .andExpect(content().contentType(WebTestConstants.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(ID.intValue())))
                .andExpect(jsonPath("$[0].creationTime", is(CREATION_TIME)))
                .andExpect(jsonPath("$[0].description", is(DESCRIPTION)))
                .andExpect(jsonPath("$[0].modificationTime", is(MODIFICATION_TIME)))
                .andExpect(jsonPath("$[0].title", is(TITLE)));

        ArgumentCaptor<Pageable> pageableArgument = ArgumentCaptor.forClass(Pageable.class);

        verify(todoSearchServiceMock, times(1)).findBySearchTerm(eq(SEARCH_TERM), pageableArgument.capture());
        verifyNoMoreInteractions(todoSearchServiceMock);
        verifyZeroInteractions(todoCrudServiceMock);

        Pageable pageSpecification = pageableArgument.getValue();

        assertThat(pageSpecification.getPageNumber()).isEqualTo(PAGE_NUMBER);
        assertThat(pageSpecification.getPageSize()).isEqualTo(PAGE_SIZE);

        Sort sortSpecification = pageSpecification.getSort();
        assertThat(sortSpecification.getOrderFor(SORT_FIELD).getDirection()).isEqualTo(Sort.Direction.fromString(SORT_ORDER));
    }

    @Test
    public void update_EmptyTodoEntry_ShouldReturnValidationErrorAboutMissingTitleAsJsonDocument() throws Exception {
        TodoDTO updatedTodoEntry = new TodoDTO();

        mockMvc.perform(put("/api/todo/{id}", ID)
                .contentType(WebTestConstants.APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsBytes(updatedTodoEntry))
        )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(WebTestConstants.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.name())))
                .andExpect(jsonPath("$.code", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.message", not(isEmptyOrNullString())))
                .andExpect(jsonPath("$.validationErrors", hasSize(1)))
                .andExpect(jsonPath("$.validationErrors[?(@.field=='title')].errorCode", contains(WebTestConstants.ERROR_CODE_NOT_EMPTY)))
                .andExpect(jsonPath("$.validationErrors[?(@.field=='title')].errorMessage", not(isEmptyOrNullString())));

        verifyZeroInteractions(todoCrudServiceMock, todoSearchServiceMock);
    }

    @Test
    public void update_TitleAndDescriptionAreTooLong_ShouldReturnValidationErrorsAboutTitleAndDescriptionAsJsonDocument() throws Exception {
        String tooLongTitle = WebTestUtil.createStringWithLength(101);
        String tooLongDescription = WebTestUtil.createStringWithLength(501);

        TodoDTO updatedTodoEntry = new TodoDTOBuilder()
                .description(tooLongDescription)
                .title(tooLongTitle)
                .build();

        mockMvc.perform(put("/api/todo/{id}", ID)
                .contentType(WebTestConstants.APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsBytes(updatedTodoEntry))
        )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(WebTestConstants.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.status", is(HttpStatus.BAD_REQUEST.name())))
                .andExpect(jsonPath("$.code", is(HttpStatus.BAD_REQUEST.value())))
                .andExpect(jsonPath("$.message", not(isEmptyOrNullString())))
                .andExpect(jsonPath("$.validationErrors", hasSize(2)))
                .andExpect(jsonPath("$.validationErrors[?(@.field=='description')].errorCode", contains(WebTestConstants.ERROR_CODE_LENGTH)))
                .andExpect(jsonPath("$.validationErrors[?(@.field=='description')].errorMessage", not(isEmptyOrNullString())))
                .andExpect(jsonPath("$.validationErrors[?(@.field=='title')].errorCode", contains(WebTestConstants.ERROR_CODE_LENGTH)))
                .andExpect(jsonPath("$.validationErrors[?(@.field=='title')].errorMessage", not(isEmptyOrNullString())));

        verifyZeroInteractions(todoCrudServiceMock, todoSearchServiceMock);
    }

    @Test
    public void update_TodoEntryNotFound_ShouldReturnHttpStatusCodeNotFound() throws Exception {
        TodoDTO updatedTodoEntry = new TodoDTOBuilder()
                .description(DESCRIPTION)
                .title(TITLE)
                .build();

        when(todoCrudServiceMock.update(isA(TodoDTO.class))).thenThrow(new TodoNotFoundException(""));

        mockMvc.perform(put("/api/todo/{id}", ID)
                .contentType(WebTestConstants.APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsBytes(updatedTodoEntry))
        )
                .andExpect(status().isNotFound());

        ArgumentCaptor<TodoDTO> serviceMethodArgument = ArgumentCaptor.forClass(TodoDTO.class);

        verify(todoCrudServiceMock, times(1)).update(serviceMethodArgument.capture());
        verifyNoMoreInteractions(todoCrudServiceMock);
        verifyZeroInteractions(todoSearchServiceMock);

        TodoDTO serviceMethodArgumentValue = serviceMethodArgument.getValue();
        assertThatTodoDTO(serviceMethodArgumentValue)
                .hasId(ID)
                .hasNoCreationTime()
                .hasDescription(DESCRIPTION)
                .hasNoModificationTime()
                .hasTitle(TITLE);
    }

    @Test
    public void update_TodoEntryFound_ShouldReturnUpdatedTodoEntryAsJsonDocument() throws Exception {
        TodoDTO updatedTodoEntry = new TodoDTOBuilder()
                .description(DESCRIPTION)
                .title(TITLE)
                .build();

        TodoDTO returnedTodoEntry = new TodoDTOBuilder()
                .creationTime(CREATION_TIME)
                .description(DESCRIPTION)
                .id(ID)
                .modificationTime(MODIFICATION_TIME)
                .title(TITLE)
                .build();

        when(todoCrudServiceMock.update(isA(TodoDTO.class))).thenReturn(returnedTodoEntry);

        mockMvc.perform(put("/api/todo/{id}", ID)
                        .contentType(WebTestConstants.APPLICATION_JSON_UTF8)
                        .content(objectMapper.writeValueAsBytes(updatedTodoEntry))
        )
                .andExpect(status().isOk())
                .andExpect(content().contentType(WebTestConstants.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.id", is(ID.intValue())))
                .andExpect(jsonPath("$.creationTime", is(CREATION_TIME)))
                .andExpect(jsonPath("$.description", is(DESCRIPTION)))
                .andExpect(jsonPath("$.modificationTime", is(MODIFICATION_TIME)))
                .andExpect(jsonPath("$.title", is(TITLE)));

        ArgumentCaptor<TodoDTO> serviceMethodArgument = ArgumentCaptor.forClass(TodoDTO.class);

        verify(todoCrudServiceMock, times(1)).update(serviceMethodArgument.capture());
        verifyNoMoreInteractions(todoCrudServiceMock);
        verifyZeroInteractions(todoSearchServiceMock);

        TodoDTO serviceMethodArgumentValue = serviceMethodArgument.getValue();
        assertThatTodoDTO(serviceMethodArgumentValue)
                .hasId(ID)
                .hasNoCreationTime()
                .hasDescription(DESCRIPTION)
                .hasNoModificationTime()
                .hasTitle(TITLE);
    }
}
