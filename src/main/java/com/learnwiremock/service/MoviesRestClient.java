package com.learnwiremock.service;

import com.learnwiremock.constants.MoviesAppConstants;
import com.learnwiremock.dto.Movie;
import com.learnwiremock.exception.MovieErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Slf4j
public class MoviesRestClient {

    private WebClient webClient;

    public MoviesRestClient(WebClient webClient) {
        this.webClient = webClient;
    }


    public List<Movie> retriveAllMovies() {
        try{
            // http://localhost:8081/movieservice/v1/allMovies
            return webClient.get().uri(MoviesAppConstants.GET_ALL_MOVIES_V1)
                    .retrieve()
                    .bodyToFlux(Movie.class)
                    .collectList()
                    .block();
        }
        catch (WebClientResponseException e) {
            log.error("WebClientResponseException in retriveMovieById. Status code is {} and the message is {} ",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new MovieErrorResponse(e.getStatusText(), e);
        } catch (Exception ex) {
            log.error("Exception in retrieveMovieById and the message is {}", ex);
            throw new MovieErrorResponse(ex);
        }


    }

    public Movie retriveMovieById(Integer movieId) {

        // http://localhost:8081/movieservice/v1/movie/1
        try {
            return webClient.get().uri(MoviesAppConstants.MOVIE_BY_ID_PATH_PARAM_V1, movieId)
                    .retrieve()
                    .bodyToMono(Movie.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("WebClientResponseException in retriveMovieById. Status code is {} and the message is {} ",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new MovieErrorResponse(e.getStatusText(), e);
        } catch (Exception e) {
            log.error("Exception in retrieveMovieById and the message is {}", e);
            throw new MovieErrorResponse((WebClientResponseException) e);
        }
    }

    public List<Movie> retrieveMovieByName(String name) {
        // http://localhost:8081/movieservice/v1/movieName?movie_name=Batman%20Begins

        String retrieveByNameUri = UriComponentsBuilder.fromUriString(MoviesAppConstants.MOVIE_BY_NAME_QUERY_PARAM_V1)
                .queryParam("movie_name", name)
                .buildAndExpand()
                .toUriString();
        try {
            return webClient.get().uri(retrieveByNameUri)
                    .retrieve()
                    .bodyToFlux(Movie.class)
                    .collectList()
                    .block();
        } catch (WebClientResponseException e) {
            log.error("WebClientResponseException in retriveMovieByName. Status code is {} and the message is {} ",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new MovieErrorResponse(e.getStatusText(), e);
        } catch (Exception e) {
            log.error("Exception in retrieveMovieByName and the message is {}", e);
            throw new MovieErrorResponse((WebClientResponseException) e);
        }
    }

    public List<Movie> retrieveMovieByYear(Integer movieYear) {
        // http://localhost:8081/movieservice/v1/movieYear?year=1800

        String retrieveByYearUri = UriComponentsBuilder.fromUriString(MoviesAppConstants.MOVIE_BY_YEAR_QUERY_PARAM_V1)
                .queryParam("year", movieYear)
                .buildAndExpand()
                .toUriString();
        try {
            return webClient.get().uri(retrieveByYearUri)
                    .retrieve()
                    .bodyToFlux(Movie.class)
                    .collectList()
                    .block();
        } catch (WebClientResponseException e) {
            log.error("WebClientResponseException in retriveMovieByYear. Status code is {} and the message is {} ",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new MovieErrorResponse(e.getStatusText(), e);
        } catch (Exception e) {
            log.error("Exception in retrieveMovieByYear and the message is {}", e);
            throw new MovieErrorResponse((WebClientResponseException) e);
        }
    }

    public Movie addMovie(Movie movie) {

        try {
            // http://localhost:8081/movieservice/v1/movie
           return webClient.post().uri(MoviesAppConstants.ADD_MOVIE_V1)
                    .syncBody(movie)
                    .retrieve()
                    .bodyToMono(Movie.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("WebClientResponseException in addMovie. Status code is {} and the message is {} ",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new MovieErrorResponse(e.getStatusText(), e);
        } catch (Exception e) {
            log.error("Exception in addMovie and the message is {}", e);
            throw new MovieErrorResponse((WebClientResponseException) e);
        }
    }

    public Movie updateMovie(Integer movieId, Movie movie) {

        try {
            return webClient.put().uri(MoviesAppConstants.MOVIE_BY_ID_PATH_PARAM_V1, movieId)
                    .syncBody(movie)
                    .retrieve().bodyToMono(Movie.class).block();
        } catch (WebClientResponseException e) {
            log.error("WebClientResponseException in updateMovie. Status code is {} and the message is {} ",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new MovieErrorResponse(e.getStatusText(), e);
        } catch (Exception e) {
            log.error("Exception in updateMovie and the message is {}", e);
            throw new MovieErrorResponse((WebClientResponseException) e);
        }
}

    public String deleteMovie(Integer movieId) {
        try{
            return webClient.delete().uri(MoviesAppConstants.MOVIE_BY_ID_PATH_PARAM_V1, movieId).retrieve()
                    .bodyToMono(String.class).block();

        }catch (WebClientResponseException e) {
            log.error("WebClientResponseException in updateMovie. Status code is {} and the message is {} ",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new MovieErrorResponse(e.getStatusText(), e);
        } catch (Exception e) {
            log.error("Exception in updateMovie and the message is {}", e);
            throw new MovieErrorResponse((WebClientResponseException) e);
        }
    }

    public String deleteMovieByName(String movieName) {
        try{
            String deleteMovieByNameURI = UriComponentsBuilder.fromUriString(MoviesAppConstants.MOVIE_BY_NAME_QUERY_PARAM_V1)
                            .queryParam("movie_name", movieName)
                                    .buildAndExpand().toUriString();

            webClient.delete().uri(deleteMovieByNameURI).retrieve()
                    .bodyToMono(Void.class).block();

        }catch (WebClientResponseException e) {
            log.error("WebClientResponseException in updateMovie. Status code is {} and the message is {} ",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new MovieErrorResponse(e.getStatusText(), e);
        } catch (Exception e) {
            log.error("Exception in updateMovie and the message is {}", e);
            throw new MovieErrorResponse((WebClientResponseException) e);
        }
        return "Movie Deleted Successfully";
    }

}