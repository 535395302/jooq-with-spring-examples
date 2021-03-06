package net.petrikainulainen.spring.jooq.todo.repository;

import net.petrikainulainen.spring.jooq.common.service.DateTimeService;
import net.petrikainulainen.spring.jooq.todo.db.tables.records.TodosRecord;
import net.petrikainulainen.spring.jooq.todo.exception.TodoNotFoundException;
import net.petrikainulainen.spring.jooq.todo.model.Todo;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static net.petrikainulainen.spring.jooq.todo.db.tables.Todos.TODOS;

/**
 * @author Petri Kainulainen
 */
@Repository
public class JOOQTodoRepository implements TodoRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JOOQTodoRepository.class);

    private final DateTimeService dateTimeService;

    private final DSLContext jooq;

    @Autowired
    public JOOQTodoRepository(DateTimeService dateTimeService, DSLContext jooq) {
        this.dateTimeService = dateTimeService;
        this.jooq = jooq;
    }

    @Transactional
    @Override
    public Todo add(Todo todoEntry) {
        LOGGER.info("Adding new todo entry with information: {}", todoEntry);
        TodosRecord persisted = jooq.insertInto(TODOS)
                .set(createRecord(todoEntry))
                .returning()
                .fetchOne();

        Todo returned = convertQueryResultToModelObject(persisted);

        LOGGER.info("Added {} todo entry", returned);

        return returned;
    }

    private TodosRecord createRecord(Todo todoEntry) {
        Timestamp currentTime = dateTimeService.getCurrentTimestamp();
        LOGGER.debug("The current time is: {}", currentTime);

        TodosRecord record = new TodosRecord();

        record.setCreationTime(currentTime);
        record.setDescription(todoEntry.getDescription());
        record.setModificationTime(currentTime);
        record.setTitle(todoEntry.getTitle());

        return record;
    }

    @Transactional
    @Override
    public Todo delete(Long id) {
        LOGGER.info("Deleting todo entry by id: {}", id);

        Todo deleted = findById(id);

        int deletedRecordCount = jooq.delete(TODOS)
                .where(TODOS.ID.equal(id))
                .execute();

        LOGGER.debug("Deleted {} todo entries", deletedRecordCount);
        LOGGER.info("Returning deleted todo entry: {}", deleted);

        return deleted;
    }

    @Transactional(readOnly = true)
    @Override
    public List<Todo> findAll() {
        LOGGER.info("Finding all todo entries.");

        List<TodosRecord> queryResults = jooq.selectFrom(TODOS).fetchInto(TodosRecord.class);

        List<Todo> todoEntries = convertQueryResultsToModelObjects(queryResults);

        LOGGER.info("Found {} todo entries", todoEntries.size());

        return todoEntries;
    }

    @Transactional(readOnly = true)
    @Override
    public Todo findById(Long id) {
        LOGGER.info("Finding todo entry by id: {}", id);

        TodosRecord queryResult = jooq.selectFrom(TODOS)
                .where(TODOS.ID.equal(id))
                .fetchOne();

        LOGGER.debug("Got result: {}", queryResult);

        if (queryResult == null) {
            throw new TodoNotFoundException("No todo entry found with id: " + id);
        }

        return convertQueryResultToModelObject(queryResult);
    }

    @Transactional(readOnly = true)
    @Override
    public List<Todo> findBySearchTerm(String searchTerm, Pageable pageable) {
        LOGGER.info("Finding {} todo entries for page {} by using search term: {}",
                pageable.getPageSize(),
                pageable.getPageNumber(),
                searchTerm
        );

        String likeExpression = "%" + searchTerm + "%";

        List<TodosRecord> queryResults = jooq.selectFrom(TODOS)
                .where(
                        TODOS.DESCRIPTION.likeIgnoreCase(likeExpression)
                                .or(TODOS.TITLE.likeIgnoreCase(likeExpression))
                )
                .limit(pageable.getPageSize()).offset(pageable.getOffset())
                .fetchInto(TodosRecord.class);

        List<Todo> todoEntries = convertQueryResultsToModelObjects(queryResults);

        LOGGER.info("Found {} todo entries", todoEntries.size());

        return todoEntries;
    }

    private List<Todo> convertQueryResultsToModelObjects(List<TodosRecord> queryResults) {
        List<Todo> todoEntries = new ArrayList<>();

        for (TodosRecord queryResult: queryResults) {
            Todo todoEntry = convertQueryResultToModelObject(queryResult);
            todoEntries.add(todoEntry);
        }

        return todoEntries;
    }

    private Todo convertQueryResultToModelObject(TodosRecord queryResult) {
        return Todo.getBuilder(queryResult.getTitle())
                .creationTime(queryResult.getCreationTime())
                .description(queryResult.getDescription())
                .id(queryResult.getId())
                .modificationTime(queryResult.getModificationTime())
                .build();
    }

    @Transactional
    @Override
    public Todo update(Todo todoEntry) {
        LOGGER.info("Updating todo: {}", todoEntry);

        Timestamp currentTime = dateTimeService.getCurrentTimestamp();
        LOGGER.debug("The current time is: {}", currentTime);

        int updatedRecordCount = jooq.update(TODOS)
                .set(TODOS.DESCRIPTION, todoEntry.getDescription())
                .set(TODOS.MODIFICATION_TIME, currentTime)
                .set(TODOS.TITLE, todoEntry.getTitle())
                .where(TODOS.ID.equal(todoEntry.getId()))
                .execute();

        LOGGER.debug("Updated {} todo entry.", updatedRecordCount);

        //If you are using Firebird or PostgreSQL databases, you can use the RETURNING
        //clause in the update statement (and avoid the extra select clause):
        //http://www.jooq.org/doc/3.2/manual/sql-building/sql-statements/update-statement/#N11102

        return findById(todoEntry.getId());
    }
}
