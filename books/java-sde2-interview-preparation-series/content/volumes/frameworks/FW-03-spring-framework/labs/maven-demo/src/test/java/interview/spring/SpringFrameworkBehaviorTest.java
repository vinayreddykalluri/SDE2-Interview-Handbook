package interview.spring;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.sql.DataSource;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SpringFrameworkBehaviorTest {
    private static AnnotationConfigApplicationContext context;
    private static JdbcTemplate jdbc;
    private static EventRecorder events;
    private static CountingAspect aspect;

    @BeforeAll
    static void startContext() {
        context = new AnnotationConfigApplicationContext(BookConfiguration.class);
        jdbc = context.getBean(JdbcTemplate.class);
        events = context.getBean(EventRecorder.class);
        aspect = context.getBean(CountingAspect.class);
    }

    @AfterAll
    static void stopContext() {
        context.close();
    }

    @BeforeEach
    void resetState() {
        jdbc.update("delete from order_ledger");
        events.reset();
        aspect.reset();
    }

    @Test
    void constructorInjectionAndProviderReturnManagedObjects() {
        NotificationService notifications = context.getBean(NotificationService.class);
        assertEquals("reader@example.com: ready", notifications.format("reader@example.com"));

        TokenFactory tokens = context.getBean(TokenFactory.class);
        assertNotSame(tokens.next(), tokens.next());
    }

    @Test
    void lifecycleRunsInitializationAndDestructionCallbacks() {
        LifecycleProbe.reset();
        try (var local = new AnnotationConfigApplicationContext(LifecycleConfiguration.class)) {
            assertTrue(local.isActive());
            assertTrue(LifecycleProbe.initialized);
            assertFalse(LifecycleProbe.destroyed);
        }
        assertTrue(LifecycleProbe.destroyed);
    }

    @Test
    void eventsAreSynchronousAndAfterCommitListenerObservesOnlyCommit() {
        EventPublishingService service = context.getBean(EventPublishingService.class);
        String callingThread = Thread.currentThread().getName();

        service.publishCommitted("committed");
        assertEquals(List.of("committed"), events.synchronousKeys);
        assertEquals(List.of("committed"), events.committedKeys);
        assertEquals(callingThread, events.listenerThread);

        assertThrows(IllegalStateException.class,
                () -> service.publishRolledBack("rolled-back"));
        assertEquals(List.of("committed", "rolled-back"), events.synchronousKeys);
        assertEquals(List.of("committed"), events.committedKeys);
    }

    @Test
    void externalProxyCallIsAdvisedButSelfInvocationIsNot() {
        TrackedService service = context.getBean(TrackedService.class);
        assertTrue(AopUtils.isAopProxy(service));

        service.tracked();
        assertEquals(1, aspect.invocations());

        service.outer();
        assertEquals(1, aspect.invocations());
    }

    @Test
    void uncheckedFailureRollsBackButCheckedFailureCommitsByDefault() {
        TransactionalLedgerService service = context.getBean(TransactionalLedgerService.class);

        assertThrows(IllegalStateException.class,
                () -> service.insertThenFailUnchecked("unchecked"));
        assertEquals(0, count("unchecked"));

        assertThrows(ImportCheckedException.class,
                () -> service.insertThenFailChecked("checked"));
        assertEquals(1, count("checked"));
    }

    @Test
    void explicitCheckedRollbackRuleRollsBack() {
        TransactionalLedgerService service = context.getBean(TransactionalLedgerService.class);
        assertThrows(ImportCheckedException.class,
                () -> service.insertThenFailCheckedWithRollback("checked-rollback"));
        assertEquals(0, count("checked-rollback"));
    }

    private int count(String requestKey) {
        Integer count = jdbc.queryForObject(
                "select count(*) from order_ledger where request_key = ?",
                Integer.class,
                requestKey);
        return count == null ? 0 : count;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @EnableTransactionManagement(proxyTargetClass = true)
    static class BookConfiguration {
        @Bean
        DataSource dataSource() {
            return new DriverManagerDataSource(
                    "jdbc:h2:mem:spring_framework_book;DB_CLOSE_DELAY=-1", "sa", "");
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            JdbcTemplate template = new JdbcTemplate(dataSource);
            template.execute("""
                    create table if not exists order_ledger (
                        id bigint generated by default as identity primary key,
                        request_key varchar(100) not null unique
                    )
                    """);
            return template;
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        MessageSender messageSender() {
            return (recipient, text) -> recipient + ": " + text;
        }

        @Bean
        NotificationService notificationService(MessageSender sender) {
            return new NotificationService(sender);
        }

        @Bean
        @Scope("prototype")
        WorkToken workToken() {
            return new WorkToken();
        }

        @Bean
        TokenFactory tokenFactory(ObjectProvider<WorkToken> tokens) {
            return new TokenFactory(tokens);
        }

        @Bean
        EventRecorder eventRecorder() {
            return new EventRecorder();
        }

        @Bean
        EventPublishingService eventPublishingService(
                JdbcTemplate template, ApplicationEventPublisher publisher) {
            return new EventPublishingService(template, publisher);
        }

        @Bean
        CountingAspect countingAspect() {
            return new CountingAspect();
        }

        @Bean
        TrackedService trackedService() {
            return new TrackedService();
        }

        @Bean
        TransactionalLedgerService transactionalLedgerService(JdbcTemplate template) {
            return new TransactionalLedgerService(template);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class LifecycleConfiguration {
        @Bean(initMethod = "initialize", destroyMethod = "close")
        LifecycleProbe lifecycleProbe() {
            return new LifecycleProbe();
        }
    }

    interface MessageSender {
        String send(String recipient, String text);
    }

    record NotificationService(MessageSender sender) {
        String format(String recipient) {
            return sender.send(recipient, "ready");
        }
    }

    static final class WorkToken {
    }

    record TokenFactory(ObjectProvider<WorkToken> tokens) {
        WorkToken next() {
            return tokens.getObject();
        }
    }

    static final class LifecycleProbe {
        static boolean initialized;
        static boolean destroyed;

        static void reset() {
            initialized = false;
            destroyed = false;
        }

        void initialize() {
            initialized = true;
        }

        void close() {
            destroyed = true;
        }
    }

    record OrderRecorded(String requestKey) {
    }

    static final class EventRecorder {
        private final List<String> synchronousKeys = new ArrayList<>();
        private final List<String> committedKeys = new ArrayList<>();
        private String listenerThread;

        @EventListener
        @Order(1)
        void synchronous(OrderRecorded event) {
            synchronousKeys.add(event.requestKey());
            listenerThread = Thread.currentThread().getName();
        }

        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        void afterCommit(OrderRecorded event) {
            committedKeys.add(event.requestKey());
        }

        void reset() {
            synchronousKeys.clear();
            committedKeys.clear();
            listenerThread = null;
        }
    }

    static class EventPublishingService {
        private final JdbcTemplate jdbc;
        private final ApplicationEventPublisher publisher;

        EventPublishingService(JdbcTemplate jdbc, ApplicationEventPublisher publisher) {
            this.jdbc = jdbc;
            this.publisher = publisher;
        }

        @Transactional
        public void publishCommitted(String requestKey) {
            recordAndPublish(requestKey);
        }

        @Transactional
        public void publishRolledBack(String requestKey) {
            recordAndPublish(requestKey);
            throw new IllegalStateException("force rollback");
        }

        private void recordAndPublish(String requestKey) {
            jdbc.update("insert into order_ledger(request_key) values (?)", requestKey);
            publisher.publishEvent(new OrderRecorded(requestKey));
        }
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface TrackedOperation {
    }

    @Aspect
    static final class CountingAspect {
        private final AtomicInteger calls = new AtomicInteger();

        @Around("@annotation(interview.spring.SpringFrameworkBehaviorTest.TrackedOperation)")
        Object count(ProceedingJoinPoint call) throws Throwable {
            calls.incrementAndGet();
            return call.proceed();
        }

        int invocations() {
            return calls.get();
        }

        void reset() {
            calls.set(0);
        }
    }

    static class TrackedService {
        public void outer() {
            tracked();
        }

        @TrackedOperation
        public void tracked() {
        }
    }

    static class TransactionalLedgerService {
        private final JdbcTemplate jdbc;

        TransactionalLedgerService(JdbcTemplate jdbc) {
            this.jdbc = jdbc;
        }

        @Transactional
        public void insertThenFailUnchecked(String requestKey) {
            insert(requestKey);
            throw new IllegalStateException("unchecked failure");
        }

        @Transactional
        public void insertThenFailChecked(String requestKey)
                throws ImportCheckedException {
            insert(requestKey);
            throw new ImportCheckedException("checked failure");
        }

        @Transactional(rollbackFor = ImportCheckedException.class)
        public void insertThenFailCheckedWithRollback(String requestKey)
                throws ImportCheckedException {
            insert(requestKey);
            throw new ImportCheckedException("checked rollback failure");
        }

        private void insert(String requestKey) {
            jdbc.update("insert into order_ledger(request_key) values (?)", requestKey);
        }
    }

    static final class ImportCheckedException extends Exception {
        private static final long serialVersionUID = 1L;

        ImportCheckedException(String message) {
            super(message);
        }
    }
}
