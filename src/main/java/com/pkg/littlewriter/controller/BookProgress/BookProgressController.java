package com.pkg.littlewriter.controller.BookProgress;

import com.pkg.littlewriter.domain.bookProgressHelper.BookPageProgressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class BookProgressController {
    private BookPageProgressService bookPageProgressService;

    @Autowired
    public BookProgressController(BookPageProgressService bookPageProgressService) {
        this.bookPageProgressService = bookPageProgressService;
    }
}
