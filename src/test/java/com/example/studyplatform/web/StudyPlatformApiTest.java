package com.example.studyplatform.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudyPlatformApiTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;

    @Test
    void fullScenarioThroughRestApi() throws Exception {
        long teacher = createUser("TEACHER");
        long student = createUser("STUDENT");
        long category = id(jsonPost("/api/categories", "{\"name\":\"Cat " + SEQ.incrementAndGet() + "\"}"));

        ResultActions created = send(post("/api/courses").contentType(MediaType.APPLICATION_JSON).content(
                "{\"title\":\"Hibernate\",\"categoryId\":" + category + ",\"teacherId\":" + teacher + ","
                        + "\"modules\":[{\"title\":\"M1\",\"orderIndex\":1,\"lessons\":[{\"title\":\"L1\"}]}]}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/courses/")))
                .andExpect(jsonPath("$.modules[0].lessons[0].title").value("L1"));
        JsonNode course = body(created);
        long courseId = course.get("course").get("id").asLong();
        long lessonId = course.get("modules").get(0).get("lessons").get(0).get("id").asLong();

        mvc.perform(post("/api/courses/{id}/enroll", courseId).param("user", String.valueOf(student)))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/courses/{id}/enroll", courseId).param("user", String.valueOf(student)))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/courses/{id}/students", courseId))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].studentId").value(student));

        long assignmentId = id(jsonPost("/api/lessons/" + lessonId + "/assignments",
                "{\"title\":\"HW\",\"maxScore\":100}"));
        long submissionId = id(jsonPost("/api/assignments/" + assignmentId + "/submit",
                "{\"studentId\":" + student + ",\"content\":\"my solution\"}"));
        mvc.perform(post("/api/assignments/{id}/submit", assignmentId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":" + student + ",\"content\":\"again\"}"))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/submissions/{id}/grade", submissionId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":95,\"feedback\":\"well done\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("GRADED"));
        mvc.perform(get("/api/users/{id}/assignments", student))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].score").value(95));
    }

    @Test
    void unknownCourseReturns404WithErrorBody() throws Exception {
        mvc.perform(get("/api/courses/{id}", 987654))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("id=987654")))
                .andExpect(jsonPath("$.path").value("/api/courses/987654"));
    }

    @Test
    void invalidBodyReturns400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.role").exists());
    }

    @Test
    void malformedJsonAndBadPathVariableReturn400() throws Exception {
        mvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON).content("{broken"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/courses/{id}", "abc")).andExpect(status().isBadRequest());
    }

    @Test
    void duplicateEmailReturns409() throws Exception {
        String email = "dup" + SEQ.incrementAndGet() + "@api.test";
        String body = "{\"name\":\"A\",\"email\":\"" + email + "\",\"role\":\"STUDENT\"}";

        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void wrongRoleReturns422() throws Exception {
        long student = createUser("STUDENT");
        long category = id(jsonPost("/api/categories", "{\"name\":\"Cat " + SEQ.incrementAndGet() + "\"}"));

        mvc.perform(post("/api/courses").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"title\":\"X\",\"categoryId\":" + category + ",\"teacherId\":" + student + "}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void deleteReturns204AndThenResourceIsGone() throws Exception {
        long user = createUser("STUDENT");

        mvc.perform(delete("/api/users/{id}", user)).andExpect(status().isNoContent());
        mvc.perform(get("/api/users/{id}", user)).andExpect(status().isNotFound());
    }

    @Test
    void unknownPathReturns404NotServerError() throws Exception {
        mvc.perform(get("/api/no-such-resource"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void unsupportedMethodReturns405() throws Exception {
        mvc.perform(delete("/api/categories")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void unsupportedContentTypeReturns415() throws Exception {
        mvc.perform(post("/api/categories").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void missingRequiredParameterReturns400() throws Exception {
        mvc.perform(post("/api/courses/{id}/enroll", 1)).andExpect(status().isBadRequest());
    }

    @Test
    void unknownEnumValueReturns400() throws Exception {
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A\",\"email\":\"a@b.io\",\"role\":\"WIZARD\"}"))
                .andExpect(status().isBadRequest());
    }

    private long createUser(String role) throws Exception {
        int n = SEQ.incrementAndGet();
        return id(jsonPost("/api/users", "{\"name\":\"User " + n + "\",\"email\":\"u" + n + "@api.test\",\"role\":\""
                + role + "\"}"));
    }

    private long id(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request) throws Exception {
        JsonNode node = body(send(request).andExpect(status().isCreated()));
        return node.has("id") ? node.get("id").asLong() : node.get("course").get("id").asLong();
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder jsonPost(String url, String content) {
        return post(url).contentType(MediaType.APPLICATION_JSON).content(content);
    }

    private ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        return mvc.perform(request);
    }

    private JsonNode body(ResultActions actions) throws Exception {
        return json.readTree(actions.andReturn().getResponse().getContentAsString());
    }
}
