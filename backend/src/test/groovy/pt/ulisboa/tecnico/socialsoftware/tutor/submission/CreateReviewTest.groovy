package pt.ulisboa.tecnico.socialsoftware.tutor.questionsubmission.service

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.tutor.SpockTest
import pt.ulisboa.tecnico.socialsoftware.tutor.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.tutor.exceptions.TutorException
import pt.ulisboa.tecnico.socialsoftware.tutor.question.domain.Question
import pt.ulisboa.tecnico.socialsoftware.tutor.questionsubmission.domain.QuestionSubmission
import pt.ulisboa.tecnico.socialsoftware.tutor.questionsubmission.dto.ReviewDto
import pt.ulisboa.tecnico.socialsoftware.tutor.user.User
import spock.lang.Unroll

import static pt.ulisboa.tecnico.socialsoftware.tutor.exceptions.ErrorMessage.*

@DataJpaTest
class CreateReviewTest extends SpockTest{
    def student
    def teacher
    def question
    def questionSubmission

    def setup() {
        student = new User(USER_1_NAME, USER_1_USERNAME, User.Role.STUDENT)
        student.setEnrolledCoursesAcronyms(courseExecution.getAcronym())
        userRepository.save(student)
        teacher = new User(USER_2_NAME, USER_2_USERNAME, User.Role.TEACHER)
        userRepository.save(teacher)
        question = new Question()
        question.setKey(1)
        question.setTitle(QUESTION_1_TITLE)
        question.setContent(QUESTION_1_CONTENT)
        question.setCourse(course)
        question.setStatus(Question.Status.IN_REVIEW)
        questionRepository.save(question)
        questionSubmission = new QuestionSubmission()
        questionSubmission.setQuestion(question)
        questionSubmission.setUser(student)
        questionSubmission.setCourseExecution(courseExecution)
        questionSubmissionRepository.save(questionSubmission)
    }

    @Unroll
    def "create review with review status '#reviewStatus'"() {
        given: "a reviewDto"
        def reviewDto = new ReviewDto()
        reviewDto.setQuestionSubmissionId(questionSubmission.getId())
        reviewDto.setUserId(teacher.getId())
        reviewDto.setComment(REVIEW_1_COMMENT)
        reviewDto.setStatus(reviewStatus)

        when:
        questionSubmissionService.createReview(reviewDto)

        then:
        def result = reviewRepository.findAll().get(0)
        def question = questionRepository.findAll().get(0)
        result.getId() != null
        result.getComment() == REVIEW_1_COMMENT
        result.getQuestionSubmission() == questionSubmission
        result.getUser() == teacher
        result.getStatus().name() == reviewStatus

        where:
        reviewStatus << ['AVAILABLE', 'DISABLED', 'REJECTED', 'IN_REVIEW', 'IN_REVISION', 'COMMENT']
    }

    def "create review for question submission that has already been reviewed"() {
        given: "a question submission that has already been reviewed"
        question.setStatus(Question.Status.AVAILABLE)
        questionRepository.save(question)

        and: "a reviewDto"
        def reviewDto = new ReviewDto()
        reviewDto.setQuestionSubmissionId(questionSubmission.getId())
        reviewDto.setUserId(teacher.getId())
        reviewDto.setComment(REVIEW_1_COMMENT)
        reviewDto.setStatus('REJECTED')

        when:
        questionSubmissionService.createReview(reviewDto)
        then: "exception is thrown"
        def exception = thrown(TutorException)
        exception.getErrorMessage() == CANNOT_REVIEW_QUESTION_SUBMISSION
    }

    @Unroll
    def "invalid arguments: comment=#comment | hasQuestionSubmission=#hasQuestionSubmission | hasUser=#hasUser | status=#status || errorMessage"(){
        given: "a questionSubmission"
        def submission = new QuestionSubmission()
        submission.setQuestion(question)
        submission.setUser(student)
        submission.setCourseExecution(courseExecution)
        questionSubmissionRepository.save(submission)
        and: "a reviewDto"
        def reviewDto = new ReviewDto()
        reviewDto.setQuestionSubmissionId(hasQuestionSubmission ? submission.getId() : null)
        reviewDto.setUserId(hasUser ? submission.getUser().getId() : null)
        reviewDto.setComment(comment)
        reviewDto.setStatus(status)

        when:
        questionSubmissionService.createReview(reviewDto)

        then: "a TutorException is thrown"
        def exception = thrown(TutorException)
        exception.errorMessage == errorMessage

        where:
        comment           | hasQuestionSubmission  | hasUser  | status        || errorMessage
        null              | true                   | true     | 'AVAILABLE'   || REVIEW_MISSING_COMMENT
        ' '               | true                   | true     | 'AVAILABLE'   || REVIEW_MISSING_COMMENT
        REVIEW_1_COMMENT  | false                  | true     | 'AVAILABLE'   || REVIEW_MISSING_QUESTION_SUBMISSION
        REVIEW_1_COMMENT  | true                   | false    | 'AVAILABLE'   || REVIEW_MISSING_USER
        REVIEW_1_COMMENT  | true                   | true     | null          || INVALID_STATUS_FOR_QUESTION
        REVIEW_1_COMMENT  | true                   | true     | ' '           || INVALID_STATUS_FOR_QUESTION
        REVIEW_1_COMMENT  | true                   | true     | 'INVALID'     || INVALID_STATUS_FOR_QUESTION
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}


