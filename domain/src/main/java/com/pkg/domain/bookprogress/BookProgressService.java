package com.pkg.domain.bookprogress;

import com.pkg.domain.book.*;
import com.pkg.domain.uitl.UuidGen;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class BookProgressService {

    private final BookInProgressRepository bookInProgressRepository;

    public BookProgressService(
            BookInProgressRepository bookInProgressRepository
    ) {
        this.bookInProgressRepository = bookInProgressRepository;
    }

    public BookInProgress initBook(BookInitRequest request) {
        BookInProgress bookInProgress = new BookInProgress(
                UuidGen.compact(),
                request.currentUser().id(),
                request.background(),
                request.character(),
                new ArrayList<>()
        );
        bookInProgressRepository.save(bookInProgress);
        return bookInProgress;
    }

    public BookInProgress updateBookInProgress(AddBookPageRequest request) {
        BookInProgress updateTarget = request.bookInProgress();
        validateOwner(updateTarget, request);
        updateTarget.addBookPage(request.bookPage());
        bookInProgressRepository.save(updateTarget);
        return updateTarget;
    }

    public BookInProgress retrieveById(String bipId) {
        BookInProgress found = bookInProgressRepository.retrieveById(bipId);
        validateNotNull(found, bipId);
        return found;
    }


    private void validateOwner(BookInProgress bookInProgress, AddBookPageRequest request) {
        long currentUserId = request.currentUser().id();
        if(currentUserId != bookInProgress.ownerId()) {
            throw BookProgressException.forbiddenResource();
        }
    }

    private void validateNotNull(BookInProgress bookInProgress, String targetId) {
        if(bookInProgress == null) {
            throw BookProgressException.notFound(targetId);
        }
    }
}
