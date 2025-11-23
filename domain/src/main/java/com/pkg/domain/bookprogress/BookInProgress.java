package com.pkg.domain.bookprogress;

import com.pkg.domain.ai.CreateOnePageCommand;
import com.pkg.domain.book.BookPage;
import com.pkg.domain.character.BookCharacter;
import com.pkg.domain.member.Role;
import com.pkg.domain.uitl.UuidGen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

public record BookInProgress(
    String id,
    Long ownerId,
    String backgroundInfo,
    BookCharacter character,
    List<BookPage> previousPages,
    Status status
) {

    public static BookInProgress fromCommand(BookInitCommand command, BookCharacter bookCharacter) {
        return new BookInProgress(
                UuidGen.compact(),
                command.currentUser().id(),
                command.background(),
                bookCharacter,
                new ArrayList<>(),
                BookInProgress.Status.IN_PROGRESS
        );
    }

    public BookInProgress changeBookPages(UnaryOperator<List<BookPage>> pageModifier) {
        if(status == Status.COMPLETED) {
            throw BookProgressException.bookNotCompleted(id);
        }
        return new BookInProgress(
                id,
                ownerId,
                backgroundInfo,
                character,
                pageModifier.apply(previousPages),
                status
        );
    }

    public BookInProgress changeBookPage(UnaryOperator<BookPage> pageModifier) {
        if(status == Status.COMPLETED) {
            throw BookProgressException.alreadyCompleted(id);
        }
        return new BookInProgress(
                id,
                ownerId,
                backgroundInfo,
                character,
                previousPages.stream().map(pageModifier).toList(),
                status
        );
    }

    public BookInProgress addBookPage(BookPage bookPage) {
        if(status == Status.COMPLETED) {
            throw BookProgressException.bookNotCompleted(id);
        }
        List<BookPage> updated = new ArrayList<>(previousPages);
        updated.add(bookPage);
        return new BookInProgress(
                this.id,
                this.ownerId,
                this.backgroundInfo,
                this.character,
                updated,
                Status.IN_PROGRESS
        );
    }

    public BookInProgress appendPageFrom(CreateOnePageCommand command, String context, String imageUrl) {
        if(status == Status.COMPLETED) {
            throw BookProgressException.bookNotCompleted(id);
        }
        if(command.currentUser().id() != ownerId
           && !command.currentUser().role().equals(Role.ADMIN)) {
            throw BookProgressException.forbiddenResource();
        }
        List<BookPage> updated = new ArrayList<>(previousPages);
        BookPage bookPage = new BookPage(context, imageUrl, previousPages().size());
        updated.add(bookPage);
        return new BookInProgress(
                this.id,
                this.ownerId,
                this.backgroundInfo,
                this.character,
                updated,
                Status.IN_PROGRESS
        );
    }

    public BookInProgress markAsCompleted() {
        if(status == Status.COMPLETED) {
            throw BookProgressException.alreadyCompleted(id);
        }
        return new BookInProgress(
                this.id,
                this.ownerId,
                this.backgroundInfo,
                this.character,
                this.previousPages,
                Status.COMPLETED
        );
    }

    public BookInProgress markAsPending() {
        if(status == Status.COMPLETED) {
            throw BookProgressException.alreadyCompleted(id);
        }
        return new BookInProgress(
                this.id,
                this.ownerId,
                this.backgroundInfo,
                this.character,
                this.previousPages,
                Status.PENDING
        );
    }

    public enum Status {

        COMPLETED,
        IN_PROGRESS,
        PENDING
    }
}
