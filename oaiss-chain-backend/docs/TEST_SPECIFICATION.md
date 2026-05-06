# OAISS CHAIN 测试规范文档

## 目录

1. [测试分层架构](#1-测试分层架构)
2. [测试命名规范](#2-测试命名规范)
3. [测试组织结构](#3-测试组织结构)
4. [Repository层测试规范](#4-repository层测试规范)
5. [Service层测试规范](#5-service层测试规范)
6. [Controller层测试规范](#6-controller层测试规范)
7. [覆盖率要求](#7-覆盖率要求)
8. [测试最佳实践](#8-测试最佳实践)

---

## 1. 测试分层架构

### 测试金字塔

```
┌─────────────────────────────────────────┐
│   E2E Tests (@SpringBootTest)           │ ← 10-15%
│   全上下文，真实依赖                      │
├─────────────────────────────────────────┤
│   Integration Tests (@DataJpaTest,      │ ← 20-30%
│   @WebMvcTest, Testcontainers)          │
├─────────────────────────────────────────┤
│   Unit Tests (JUnit 5 + Mockito)        │ ← 60-70%
└─────────────────────────────────────────┘
```

### 测试类型定义

| 测试类型 | 注解 | 速度 | 目标 |
|---------|------|------|------|
| 单元测试 | `@Test` | 毫秒级 | 隔离测试单个类/方法逻辑 |
| Repository测试 | `@DataJpaTest` | 秒级 | 测试数据访问层 |
| Controller测试 | `@WebMvcTest` | 秒级 | 测试Web层 |
| 集成测试 | `@Testcontainers` | 秒级 | 测试组件间交互 |
| 端到端测试 | `@SpringBootTest` | 十秒级 | 测试完整业务流程 |

---

## 2. 测试命名规范

### 方法命名

```java
// 推荐：行为驱动命名
@Test
void methodName_WhenCondition_ShouldExpectedBehavior()

// 示例
@Test
void createUser_WhenEmailIsValid_ShouldCreateSuccessfully()

@Test
void withdraw_WhenBalanceIsInsufficient_ShouldThrowInsufficientFundsException()

@Test
void calculateDiscount_WhenUserIsPremium_ShouldApplyTenPercentDiscount()
```

### 类命名

| 测试类型 | 命名规则 | 示例 |
|---------|---------|------|
| 单元测试 | `{Class}Test` | `UserServiceTest` |
| Repository测试 | `{Class}Test` | `UserRepositoryTest` |
| Controller测试 | `{Class}IntegrationTest` | `UserControllerIntegrationTest` |
| E2E测试 | `{Feature}EndToEndTest` | `UserRegistrationEndToEndTest` |

### DisplayName规范

```java
@Test
@DisplayName("创建用户 - 当提供有效信息时 - 应成功创建")
void createUser_WhenValidInfo_ShouldCreateSuccessfully() {
    // ...
}

@Nested
@DisplayName("创建用户场景")
class CreateUserScenarios {
    // ...
}
```

---

## 3. 测试组织结构

### 目录结构

```
src/
├── main/
│   └── java/com/oaiss/chain/
│       ├── controller/
│       ├── service/
│       └── repository/
│
└── test/
    ├── java/com/oaiss/chain/
    │   ├── BaseIntegrationTest.java       # 集成测试基类
    │   ├── repository/                     # Repository测试
    │   │   ├── UserRepositoryTest.java
    │   │   └── EnterpriseRepositoryTest.java
    │   ├── service/                        # Service测试
    │   │   ├── UserServiceTest.java
    │   │   └── AuthServiceTest.java
    │   ├── integration/                    # 集成测试
    │   │   └── UserIntegrationTest.java
    │   └── OaissChainApplicationTests.java # 启动测试
    │
    └── resources/
        └── application-test.yml            # 测试配置
```

### 测试类组织模板

```java
@DisplayName("用户服务测试")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UserService userService;
    
    @Nested
    @DisplayName("创建用户场景")
    class CreateUserScenarios {
        
        @Test
        @DisplayName("创建用户 - 当提供有效信息时 - 应成功创建")
        void createUser_WhenValidInfo_ShouldCreateSuccessfully() {
            // Given
            // When
            // Then
        }
        
        @Test
        @DisplayName("创建用户 - 当邮箱重复时 - 应抛出异常")
        void createUser_WhenDuplicateEmail_ShouldThrowException() {
            // ...
        }
    }
    
    @Nested
    @DisplayName("更新用户场景")
    class UpdateUserScenarios {
        // Update tests
    }
}
```

---

## 4. Repository层测试规范

### 基本模板

```java
@DataJpaTest
@DisplayName("用户仓储测试")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private UserRepository userRepository;
    
    @BeforeEach
    void setUp() {
        // 准备测试数据
    }
    
    @Test
    @DisplayName("根据用户名查找 - 当用户存在时 - 应返回用户")
    void findByUsername_WhenUserExists_ShouldReturnUser() {
        // Given
        User user = User.builder()
            .username("testuser")
            .email("test@example.com")
            .build();
        entityManager.persist(user);
        entityManager.flush();
        
        // When
        Optional<User> found = userRepository.findByUsername("testuser");
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("testuser");
    }
}
```

### 测试覆盖要求

- [ ] 所有自定义查询方法必须有测试
- [ ] 测试正常场景（数据存在）
- [ ] 测试边界场景（数据不存在）
- [ ] 测试分页查询
- [ ] 测试复杂查询（@Query注解）

---

## 5. Service层测试规范

### 基本模板

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("用户服务测试")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    @DisplayName("创建用户 - 当提供有效信息时 - 应成功创建")
    void createUser_WhenValidInfo_ShouldCreateSuccessfully() {
        // Given
        UserRequest request = new UserRequest("john@example.com", "password123");
        User expectedUser = new User(1L, "john@example.com", "encodedPassword");
        
        given(userRepository.existsByEmail("john@example.com")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("encodedPassword");
        given(userRepository.save(any(User.class))).willReturn(expectedUser);
        
        // When
        User result = userService.createUser(request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        
        then(userRepository).should().existsByEmail("john@example.com");
        then(userRepository).should().save(any(User.class));
    }
}
```

### 测试覆盖要求

- [ ] 所有公共方法必须有测试
- [ ] 成功场景测试
- [ ] 异常场景测试（参数校验、业务规则校验）
- [ ] 边界条件测试
- [ ] Mock验证（调用次数、参数验证）

---

## 6. Controller层测试规范

### 基本模板

```java
@WebMvcTest(UserController.class)
@DisplayName("用户控制器测试")
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    
    @MockitoBean
    private UserService userService;
    
    @Test
    @DisplayName("获取用户 - 当用户存在时 - 应返回200和用户信息")
    void getUser_WhenUserExists_ShouldReturn200WithUser() throws Exception {
        // Given
        User user = new User(1L, "john@example.com", "John Doe");
        given(userService.findById(1L)).willReturn(user);
        
        // When & Then
        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("john@example.com"))
            .andExpect(jsonPath("$.name").value("John Doe"));
    }
    
    @Test
    @DisplayName("创建用户 - 当请求有效时 - 应返回201")
    void createUser_WhenValidRequest_ShouldReturn201() throws Exception {
        // Given
        UserRequest request = new UserRequest("new@example.com", "password");
        String requestBody = objectMapper.writeValueAsString(request);
        
        // When & Then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated());
    }
}
```

### 测试覆盖要求

- [ ] 所有HTTP端点必须有测试
- [ ] 测试HTTP状态码（200, 201, 400, 401, 403, 404, 500）
- [ ] 测试请求参数验证
- [ ] 测试响应格式
- [ ] 测试异常处理

---

## 7. 覆盖率要求

### JaCoCo配置

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <configuration>
        <rules>
            <rule>
                <element>BUNDLE</element>
                <limits>
                    <limit>
                        <counter>LINE</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.90</minimum>
                    </limit>
                    <limit>
                        <counter>BRANCH</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.85</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</plugin>
```

### 覆盖率目标

| 模块 | 行覆盖率目标 | 分支覆盖率目标 |
|------|------------|--------------|
| Service层 | ≥ 90% | ≥ 85% |
| Repository层 | ≥ 85% | ≥ 80% |
| Controller层 | ≥ 85% | ≥ 80% |
| Util层 | ≥ 95% | ≥ 90% |
| 整体项目 | ≥ 90% | ≥ 85% |

### 排除规则

以下类不纳入覆盖率统计：

- `**/config/**` - 配置类
- `**/dto/**` - 数据传输对象
- `**/entity/**` - 实体类
- `**/*Application.class` - 启动类
- `**/enums/**` - 枚举类

---

## 8. 测试最佳实践

### FIRST原则

- **F**ast：测试应快速执行（单元测试毫秒级）
- **I**ndependent：测试应相互独立
- **R**epeatable：测试应可重复执行
- **S**elf-validating：测试应自动验证结果
- **T**imely：测试应及时编写（TDD）

### 断言最佳实践

```java
// 推荐：使用AssertJ流式断言
assertThat(user).isNotNull();
assertThat(user.getEmail()).isEqualTo("test@example.com");
assertThat(users).hasSize(2).extracting("email").contains("a@test.com", "b@test.com");

// 避免：使用JUnit断言
assertEquals("test@example.com", user.getEmail());
```

### Mock最佳实践

```java
// 推荐：使用BDD风格
given(userRepository.findById(1L)).willReturn(Optional.of(user));
when(userRepository.save(any())).thenReturn(user);

// 验证Mock调用
then(userRepository).should().save(any(User.class));
then(userRepository).should(never()).delete(any());
then(userRepository).should(times(2)).findById(any());
```

### 测试数据准备

```java
@BeforeEach
void setUp() {
    // 使用Builder模式创建测试数据
    testUser = User.builder()
        .username("testuser")
        .email("test@example.com")
        .phone("13800138000")
        .status(1)
        .deleted(false)
        .build();
}
```

---

## 附录

### 运行测试命令

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=UserRepositoryTest

# 运行集成测试
mvn verify -Pintegration-test

# 生成覆盖率报告
mvn test jacoco:report

# 检查覆盖率阈值
mvn test jacoco:check
```

### 测试报告位置

- **JaCoCo HTML报告**: `target/site/jacoco/index.html`
- **JaCoCo XML报告**: `target/site/jacoco/jacoco.xml`
- **Surefire报告**: `target/surefire-reports/`

---

**文档版本**: 1.0  
**最后更新**: 2026-04-26  
**维护者**: OAISS Team
