package com.learnwiremock.service;

import com.github.jenspiegsa.wiremockextension.ConfigureWireMock;
import com.github.jenspiegsa.wiremockextension.InjectServer;
import com.github.jenspiegsa.wiremockextension.WireMockExtension;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.learnwiremock.constants.MoviesAppConstants;
import com.learnwiremock.dto.Movie;
import com.learnwiremock.exception.MovieErrorResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.learnwiremock.constants.MoviesAppConstants.ADD_MOVIE_V1;
import static com.learnwiremock.constants.MoviesAppConstants.MOVIE_BY_NAME_QUERY_PARAM_V1;

@ExtendWith(WireMockExtension.class)
public class MovieRestClientTest {

    MoviesRestClient moviesRestClient;
    WebClient webClient;
    @InjectServer
    WireMockServer wireMockServer;


    @ConfigureWireMock
    Options options = wireMockConfig()
            .port(8088)
            .notifier(new ConsoleNotifier(true))
            .extensions(new ResponseTemplateTransformer(true));


    @BeforeEach
    void setUp() {

        int port = wireMockServer.port();
        String baseUrl = String.format("http://localhost:%s/", port);
        webClient = WebClient.create(baseUrl);
        moviesRestClient = new MoviesRestClient(webClient);
        stubFor(any(anyUrl()).willReturn(aResponse().proxiedFrom("http://localhost:8081")));

    }

    @Test
    void retrieveAllMovies() {
        stubFor(get(urlPathEqualTo(MoviesAppConstants.GET_ALL_MOVIES_V1))
                        .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("allmovies.json")));
        List<Movie> movieList = moviesRestClient.retriveAllMovies();
        Assertions.assertTrue(movieList.size() > 0);
        System.out.println("movieList: " + movieList);
    }

    @Test
    void retrieveMovieById() {
        stubFor(get(urlPathMatching("/movieservice/v1/movie/[0-9]"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("movie.json")));
        Integer movieId = 2;
        Movie movie = moviesRestClient.retriveMovieById(movieId);
        Assertions.assertEquals("Batman Begins", movie.getName());
    }


    @Test
    void retrieveMovieByIdWithResponseTemplating() {
        stubFor(get(urlPathMatching("/movieservice/v1/movie/[0-9]"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("/movie-response_template.json")));
        Integer movieId = 7;
        Movie movie = moviesRestClient.retriveMovieById(movieId);
        Assertions.assertEquals(7, movie.getMovie_id().intValue());
        Assertions.assertEquals("Batman Begins", movie.getName());
    }

    @Test
    void retrieveMovieByIdNotFound() {
        Integer movieId = 100;
        Assertions.assertThrows(MovieErrorResponse.class, () -> moviesRestClient.retriveMovieById(movieId));
    }

    @Test
    void retrieveMovieByIdNotFoundWireMock() {
        stubFor(get(urlPathMatching("/movieservice/v1/movie/[0-9]+"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("/404-movie_id.json")));
        Integer movieId = 100;
        Assertions.assertThrows(MovieErrorResponse.class, () -> moviesRestClient.retriveMovieById(movieId));
    }


    @Test
    void retrieveMovieByName() {
        String movieName = "Avengers";
        List<Movie> movieList = moviesRestClient.retrieveMovieByName(movieName);
        String castExpected = "Robert Downey Jr, Chris Evans , Chris HemsWorth";
        Assertions.assertEquals(4, movieList.size());
        Assertions.assertEquals(castExpected, movieList.get(0).getCast());
    }

    @Test
    void retrieveMovieByNameWithWireMock() {

        String movieName = "Avengers";
        stubFor(get(urlEqualTo(MOVIE_BY_NAME_QUERY_PARAM_V1 + "?movie_name=" + movieName))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("avengers.json")));
        List<Movie> movieList = moviesRestClient.retrieveMovieByName(movieName);
        String castExpected = "Robert Downey Jr, Chris Evans , Chris HemsWorth";
        Assertions.assertEquals(4, movieList.size());
        Assertions.assertEquals(castExpected, movieList.get(0).getCast());
    }

    @Test
    void retrieveMovieByNameWithWireMock_Approach2() {

        String movieName = "Avengers";
        stubFor(get(urlPathEqualTo(MOVIE_BY_NAME_QUERY_PARAM_V1))
                .withQueryParam("movie_name", equalTo(movieName))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("avengers.json")));
        List<Movie> movieList = moviesRestClient.retrieveMovieByName(movieName);
        String castExpected = "Robert Downey Jr, Chris Evans , Chris HemsWorth";
        Assertions.assertEquals(4, movieList.size());
        Assertions.assertEquals(castExpected, movieList.get(0).getCast());
    }

    @Test
    void retrieveMovieByNameWithWireMock_With_RestTemplating() {

        String movieName = "Avengers";
        stubFor(get(urlPathEqualTo(MOVIE_BY_NAME_QUERY_PARAM_V1))
                .withQueryParam("movie_name", equalTo(movieName))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("movie_by_name_with_rest_templating.json")));
        List<Movie> movieList = moviesRestClient.retrieveMovieByName(movieName);
        String castExpected = "Robert Downey Jr, Chris Evans , Chris HemsWorth";
        Assertions.assertEquals(4, movieList.size());
        Assertions.assertEquals(castExpected, movieList.get(0).getCast());
    }

    @Test
    void retrieveMovieByNameNotFound() {
        String movieName = "ABC";
        Assertions.assertThrows(MovieErrorResponse.class, () -> moviesRestClient.retrieveMovieByName(movieName));
    }

    @Test
    void retrieveMovieByYar() {
        Integer movieYear = 2012;
        List<Movie> movieList = moviesRestClient.retrieveMovieByYear(movieYear);
        Assertions.assertEquals(2, movieList.size());
    }

    @Test
    void retrieveMovieByYearNotFound() {
        Integer movieYear = 1950;
        Assertions.assertThrows(MovieErrorResponse.class, () -> moviesRestClient.retrieveMovieByYear(movieYear));
    }

    @Test
    void addMovieTest() {
        Movie movie = new Movie(null, "Toy Story", "Tom Hanks", 2019, LocalDate.of(2019, 06, 20));
        Movie addedMovie = moviesRestClient.addMovie(movie);
        Assertions.assertNotNull(addedMovie.getMovie_id());
    }

    @Test
    void addMovieTestWireMock() {
        Movie movie = new Movie(null, "Toy Story", "Tom Hanks", 2019, LocalDate.of(2019, 06, 20));

        stubFor(post(urlPathEqualTo(ADD_MOVIE_V1))
                .withRequestBody(matchingJsonPath(("$.name"), equalTo("Toy Story")))
                .withRequestBody(matchingJsonPath(("$.cast"), containing("Tom")))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("addMovie.json")));

        Movie addedMovie = moviesRestClient.addMovie(movie);
        Assertions.assertNotNull(addedMovie.getMovie_id());
    }

    @Test
    void addMovieTest_responseTempleting() {
        Movie movie = new Movie(null, "Toy Story", "Tom Hanks", 2019, LocalDate.of(2019, 06, 20));

        stubFor(post(urlPathEqualTo(ADD_MOVIE_V1))
                .withRequestBody(matchingJsonPath(("$.name"), equalTo("Toy Story")))
                .withRequestBody(matchingJsonPath(("$.cast"), containing("Tom")))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("addMovie-template.json")));


        Movie addedMovie = moviesRestClient.addMovie(movie);
        Assertions.assertNotNull(addedMovie.getMovie_id());
    }




    @Test
    void addMovieTestWithoutName() {
        Movie movie = new Movie(null, null, "Tom Hanks", 2019, LocalDate.of(2019, 06, 20));

        String expectedError = "Please pass all the input fields : [name]";

        Assertions.assertThrows(MovieErrorResponse.class, () -> moviesRestClient.addMovie(movie), expectedError);
    }

    @Test
    void addMovieTestBadRequest() {
        Movie movie = new Movie(null, null, "Tom Hanks", 2019, LocalDate.of(2019, 06, 20));

        stubFor(post(urlPathEqualTo(ADD_MOVIE_V1))
                .withRequestBody(matchingJsonPath(("$.cast"), containing("Tom")))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.BAD_REQUEST.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("addMovie-template.json")));

        String expectedError = "Please pass all the input fields : [name]";

        Assertions.assertThrows(MovieErrorResponse.class, () -> moviesRestClient.addMovie(movie), expectedError);
    }

    @Test
    void updateMovie() {
        Integer movieId = 3;

        String cast = "ABC";
        Movie movie = new Movie(null, null, cast, null, null);
        Movie updatedMovie = moviesRestClient.updateMovie(movieId, movie);

        Assertions.assertTrue(updatedMovie.getCast().contains(cast));
    }

    @Test
    void updateMovieNotFound() {
        Integer movieId = 1950;

        String cast = "ABC";
        Movie movie = new Movie(null, null, cast, null, null);
        Assertions.assertThrows(MovieErrorResponse.class, () -> moviesRestClient.updateMovie(movieId, movie));
    }

    @Test
    void updateMovieTestWireMock() {
        Integer movieId = 3;
        String cast = "No cast";
        Movie movie = new Movie(null, null,  cast,  null, null);

        stubFor(put(urlPathMatching("/movieservice/v1/movie/[0-9]+"))
                .withRequestBody(matchingJsonPath(("$.cast"), containing(cast)))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("update-movie-template.json")));

        //when
        Movie updateMovie = moviesRestClient.updateMovie(movieId, movie);
        //then
        Assertions.assertTrue(updateMovie.getCast().contains(cast));
    }

    @Test
    void updateMovieTestNotFound() {
        Integer movieId = 3;
        String cast = "No cast";
        Movie movie = new Movie(null, null,  cast,  null, null);

        stubFor(put(urlPathMatching("/movieservice/v1/movie/[0-9]+"))
                .withRequestBody(matchingJsonPath(("$.cast"), containing(cast)))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));

        //then
        Assertions.assertThrows(MovieErrorResponse.class, () -> moviesRestClient.updateMovie(movieId, movie));
    }

    @Test
    void deleteMovie() {
        Movie movie = new Movie(null, "Toy Story", "Tom Hanks", 2019, LocalDate.of(2019, 06, 20));
        Movie addedMovie = moviesRestClient.addMovie(movie);
        String responseMessage =  moviesRestClient.deleteMovie(addedMovie.getMovie_id().intValue());
        String expectedErrorMessage = "Movie Deleted Successfully";
        Assertions.assertEquals(expectedErrorMessage, responseMessage);
    }

    @Test
    void deleteMovieWiremock() {
        Movie movie = new Movie(null, "Toy Story", "Tom Hanks", 2019, LocalDate.of(2019, 06, 20));

        stubFor(post(urlPathEqualTo(ADD_MOVIE_V1))
                .withRequestBody(matchingJsonPath(("$.name"), equalTo("Toy Story")))
                .withRequestBody(matchingJsonPath(("$.cast"), containing("Tom")))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("addMovie-template.json")));
        String expectedErrorMessage = "Movie Deleted Successfully";
        Movie addedMovie = moviesRestClient.addMovie(movie);

        stubFor(delete(urlPathMatching("/movieservice/v1/movie/[0-9]+"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(expectedErrorMessage)));


        String responseMessage =  moviesRestClient.deleteMovie(addedMovie.getMovie_id().intValue());
        Assertions.assertEquals(expectedErrorMessage, responseMessage);
    }

    @Test
    void deleteMovieNotFound() {
        Integer movieId = 100;
        Assertions.assertThrows(MovieErrorResponse.class, () -> moviesRestClient.deleteMovie(movieId));
    }


    @Test
    void deleteMovieWireMockNotFound() {
        Integer movieId = 100;
        stubFor(delete(urlPathMatching("/movieservice/v1/movie/[0-9]+"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
        Assertions.assertThrows(MovieErrorResponse.class, () -> moviesRestClient.deleteMovie(movieId));
    }

    @Test
    void deleteMovieByName() {
        Movie movie = new Movie(null, "Toy Story", "Tom Hanks", 2019, LocalDate.of(2019, 06, 20));

        stubFor(post(urlPathEqualTo(ADD_MOVIE_V1))
                .withRequestBody(matchingJsonPath(("$.name"), equalTo("Toy Story")))
                .withRequestBody(matchingJsonPath(("$.cast"), containing("Tom")))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("addMovie-template.json")));
        String expectedErrorMessage = "Movie Deleted Successfully";
        Movie addedMovie = moviesRestClient.addMovie(movie);

        stubFor(delete(urlEqualTo(MOVIE_BY_NAME_QUERY_PARAM_V1 + "?movie_name=Toy%20Story"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));


        String responseMessage =  moviesRestClient.deleteMovieByName(addedMovie.getName());
        Assertions.assertEquals(expectedErrorMessage, responseMessage);

        verify(exactly(1), postRequestedFor(urlPathEqualTo(ADD_MOVIE_V1))
                .withRequestBody(matchingJsonPath(("$.name"), equalTo("Toy Story")))
                .withRequestBody(matchingJsonPath(("$.cast"), containing("Tom"))));

        verify(exactly(1), deleteRequestedFor(urlEqualTo(MOVIE_BY_NAME_QUERY_PARAM_V1 + "?movie_name=Toy%20Story")));
    }

    @Test
    void deleteMovieByName_selective_Proxying() {
        Movie movie = new Movie(null, "Toy Story", "Tom Hanks", 2019, LocalDate.of(2019, 06, 20));

        String expectedErrorMessage = "Movie Deleted Successfully";
        Movie addedMovie = moviesRestClient.addMovie(movie);

        stubFor(delete(urlEqualTo(MOVIE_BY_NAME_QUERY_PARAM_V1 + "?movie_name=Toy%20Story"))
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));


        String responseMessage =  moviesRestClient.deleteMovieByName(addedMovie.getName());
        Assertions.assertEquals(expectedErrorMessage, responseMessage);

        verify(exactly(1), deleteRequestedFor(urlEqualTo(MOVIE_BY_NAME_QUERY_PARAM_V1 + "?movie_name=Toy%20Story")));
    }




}
