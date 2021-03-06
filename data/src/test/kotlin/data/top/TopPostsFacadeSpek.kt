package data.top

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.reset
import com.nhaarman.mockito_kotlin.whenever
import dagger.Component
import dagger.Module
import dagger.Provides
import data.ComponentHolder
import data.common.DataPost
import data.common.DomainEntityMapper
import domain.entity.Post
import domain.entity.TimeRange
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.jetbrains.spek.api.SubjectSpek
import org.jetbrains.spek.api.dsl.it
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import javax.inject.Singleton

/**
 * Unit tests for TopPostsFacade.
 * @see TopPostsFacade
 */
@RunWith(JUnitPlatform::class)
internal class TopPostsFacadeSpek : SubjectSpek<TopPostsFacade>({
    subject { TopPostsFacade() }

    beforeEachTest {
        ComponentHolder.topPostsFacadeComponent = DaggerTopPostsFacadeModuleSpekDataComponent
                .builder()
                .topPostsFacadeSpekModule(TopPostsFacadeSpekModule(MOCK_ENTITY_MAPPER, MOCK_SOURCE))
                .build()
    }

    afterEachTest {
        reset(MOCK_ENTITY_MAPPER, MOCK_SOURCE)
    }

    it ("should return an observable of domain posts upon successful fetch") {
        val postOne = mock<DataPost>()
        val postTwo = mock<DataPost>()
        val postThree = mock<DataPost>()
        val postContainerOne = mock<DataPostContainer> { on { data } doReturn postOne }
        val postContainerTwo = mock<DataPostContainer> { on { data } doReturn postTwo }
        val postContainerThree = mock<DataPostContainer> { on { data } doReturn postThree }
        val requestData = mock<TopRequestData> {
            on { children } doReturn listOf(postContainerOne, postContainerTwo, postContainerThree)
        }
        val container = mock<TopRequestDataContainer> {
            on { data } doReturn requestData
        }
        val expectedTransformations = listOf<Post>(mock(), mock(), mock())
        whenever(MOCK_ENTITY_MAPPER.transform(eq(postOne))) doReturn expectedTransformations[0]
        whenever(MOCK_ENTITY_MAPPER.transform(eq(postTwo))) doReturn expectedTransformations[1]
        whenever(MOCK_ENTITY_MAPPER.transform(eq(postThree))) doReturn expectedTransformations[2]
        whenever(MOCK_SOURCE.fetch(any())) doReturn Single.just(container)
        val testSubscriber = TestObserver<Iterable<Post>>()
        // Parameters do not matter because of the mocked method on the provided store
        subject.fetchTop("", TimeRange.ALL_TIME, 0).subscribe(testSubscriber)
        testSubscriber.assertNoErrors()
        testSubscriber.assertValues(expectedTransformations)
        testSubscriber.assertComplete()
    }

    it ("should return an observable of domain posts upon successful get") {
        val postOne = mock<DataPost>()
        val postTwo = mock<DataPost>()
        val postThree = mock<DataPost>()
        val postContainerOne = mock<DataPostContainer> { on { data } doReturn postOne }
        val postContainerTwo = mock<DataPostContainer> { on { data } doReturn postTwo }
        val postContainerThree = mock<DataPostContainer> { on { data } doReturn postThree }
        val requestData = mock<TopRequestData> {
            on { children } doReturn listOf(postContainerOne, postContainerTwo, postContainerThree)
        }
        val container = mock<TopRequestDataContainer> {
            on { data } doReturn requestData
        }
        val expectedTransformations = listOf<Post>(mock(), mock(), mock())
        whenever(MOCK_ENTITY_MAPPER.transform(eq(postOne))) doReturn expectedTransformations[0]
        whenever(MOCK_ENTITY_MAPPER.transform(eq(postTwo))) doReturn expectedTransformations[1]
        whenever(MOCK_ENTITY_MAPPER.transform(eq(postThree))) doReturn expectedTransformations[2]
        whenever(MOCK_SOURCE.get(any())) doReturn Single.just(container)
        val testSubscriber = TestObserver<Iterable<Post>>()
        // Parameters do not matter because of the mocked method on the provided store
        subject.getTop("", TimeRange.ALL_TIME, 0).subscribe(testSubscriber)
        testSubscriber.assertNoErrors()
        testSubscriber.assertValues(expectedTransformations)
        testSubscriber.assertComplete()
    }

    it ("should return an observable with a propagated exception upon failed fetch") {
        val expectedError = mock<Exception>()
        whenever(MOCK_SOURCE.fetch(any())) doReturn Single.error(expectedError)
        val testSubscriber = TestObserver<Iterable<Post>>()
        // Parameters do not matter because of the mocked method on the provided store
        subject.fetchTop("", TimeRange.ALL_TIME, 0).subscribe(testSubscriber)
        testSubscriber.assertNoValues()
        testSubscriber.assertError(expectedError)
        testSubscriber.assertNotComplete()
    }

    it ("should return an observable with a propagated exception upon failed get") {
        val expectedError = mock<Exception>()
        whenever(MOCK_SOURCE.get(any())) doReturn Single.error(expectedError)
        val testSubscriber = TestObserver<Iterable<Post>>()
        // Parameters do not matter because of the mocked method on the provided store
        subject.getTop("", TimeRange.ALL_TIME, 0).subscribe(testSubscriber)
        testSubscriber.assertNoValues()
        testSubscriber.assertError(expectedError)
        testSubscriber.assertNotComplete()
    }
}) {
    private companion object {
        val MOCK_ENTITY_MAPPER = mock<DomainEntityMapper>()
        val MOCK_SOURCE = mock<TopRequestSource>()
    }
}

/**
 * Module used to provide stuff required by this Spek.
 */
@Module
internal class TopPostsFacadeSpekModule(
        private val entityMapper: DomainEntityMapper, private val source: TopRequestSource) {
    @Provides
    @Singleton
    fun entityMapper() = entityMapper

    @Provides
    @Singleton
    fun source() = source
}

/**
 * The reason why we use a replacement component instead of inheritance in the module structure
 * is that such a solution could have some potentially bad consequences.
 * @see <a href="https://google.github.io/dagger/testing.html">Testing with Dagger</a>
 */
@Component(modules = arrayOf(TopPostsFacadeSpekModule::class))
@Singleton
internal interface TopPostsFacadeModuleSpekDataComponent : TopPostsFacadeComponent
