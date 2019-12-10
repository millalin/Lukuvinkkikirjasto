package dtt.lukuvinkkikirjasto.demo.controller;

import dtt.lukuvinkkikirjasto.demo.bookdata.BookDto;
import dtt.lukuvinkkikirjasto.demo.bookdata.IsbnApiCaller;
import dtt.lukuvinkkikirjasto.demo.dao.BookDao;
import dtt.lukuvinkkikirjasto.demo.domain.Book;
import dtt.lukuvinkkikirjasto.demo.domain.Fireworks;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.sql.SQLException;

import dtt.lukuvinkkikirjasto.demo.service.BookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class BookController {

    Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private BookDao bookDao;
    private Fireworks newbookFireworks = new Fireworks();
    private IsbnApiCaller isbnApiCaller;

    @Autowired
    private BookService bookService;

    public BookController(BookDao dao, IsbnApiCaller isbnApiCaller) {
        this.bookDao = dao;
        this.isbnApiCaller = isbnApiCaller;
        isbnApiCaller.setRestTemplate(new RestTemplate());
    }

    public void setDao(BookDao dao) {
        this.bookDao = dao;
    }

    @Cacheable("bookdata")
    @RequestMapping(value = {"/books/info/{bookId}"})
    public String infoPage(Model model, @PathVariable String bookId) throws SQLException {
        try {
            Book book = bookDao.findById(bookId);
            if (book.getIsbn().equals("error") || book.getIsbn().equals("")){
                return "redirect:/books/" + bookId;
            }
            BookDto bookDto = isbnApiCaller.getBookDataFromIsbn(book.getIsbn());
            model.addAttribute("dto", bookDto);
            model.addAttribute("book", book);
            return "book";
        } catch (IOException e){
            logger.warn("Error in fetching book information. {}", e.getMessage());
        }
        return "redirect:/";
    }

    @RequestMapping(value = {"/books/{bookId}"})
    public String singleBook(Model model, @PathVariable String bookId) throws SQLException {
        Book book = bookDao.findById(bookId);
        model.addAttribute("book", book);
        return "book";
    }

    @RequestMapping(value = {"/", "/books"})
    public String frontPage(Model model, @ModelAttribute Book book) throws SQLException {
        model = bookService.returnModelForBook(model, bookDao, book, false,newbookFireworks.isNew());
        return "books";
    }

    @PostMapping("/books")
    public String saveBook(Model model, @Valid @ModelAttribute Book book, BindingResult bindingResult) throws SQLException, IOException {
        if (!Book.validate(book)){
            bindingResult.rejectValue("title", "error.book", "Must inlude either Title or valid ISBN");
        }
        if (bookDao.findByIsbn(book.getIsbn()) != null) {
            bindingResult.rejectValue("isbn", "error.book", "Book with this ISBN already added.");
        }

        if (bindingResult.hasErrors()) {
            model = bookService.returnModelForBook(model, bookDao, book, false,false);
            return "books";
        }
        book = bookService.ifIsbnFillBookInfo(book);
        book.setRead(false);
        newbookFireworks = new Fireworks();
        model = bookService.returnModelForBook(model, bookDao, book, false,newbookFireworks.isNew());
        bookDao.create(book);
        return "redirect:/";
    }

    @PostMapping("/books/delete/{bookId}")
    public String deleteBook(@PathVariable String bookId) throws SQLException {
        Book book = bookDao.findById(bookId);
        bookDao.delete(book);
        return "redirect:/";
    }

    @PostMapping("/books/editread/{bookId}")
    public String markreadBook(Model model, @PathVariable String bookId) throws SQLException {
        Book book = bookDao.findById(bookId);

        book.setRead(!book.isRead());
        bookDao.updateRead(book);

        return "redirect:/";
    }

    @GetMapping("/books/edit/{id}")
    public String editBook(Model model, @PathVariable(value = "id") String id) throws SQLException {
        Book book = bookDao.findById(id);
        if (book != null) {
            if (book.getIsbn() == "error") {
                book.setIsbn("");
            }

            model = bookService.returnModelForBook(model, bookDao, book, true, false);

            return "books";
        }
        return "redirect:/";
    }

    @PostMapping("/books/edit/{id}")
    public String restfullyEditBook(Model model, @Valid @ModelAttribute Book book, BindingResult bindingResult, RedirectAttributes redirectAttributes) throws SQLException, IOException {
        Book old = bookDao.findByIsbn(book.getIsbn());

        if (!Book.validate(book)) {
            bindingResult.rejectValue("title", "error.book", "Must inlude either Title or valid ISBN");
        }

        if (old != null && !old.getId().equals(book.getId())) {
            bindingResult.rejectValue("isbn", "error.book", "Book with this ISBN already added.");
        }

        if (bindingResult.hasErrors()) {
            model = bookService.returnModelForBook(model, bookDao, book, true, false);
            return "books";
        }
        book = bookService.ifIsbnFillBookInfo(book);
        boolean successfullyEdited = bookDao.update(book);
        if (successfullyEdited) {
            return "redirect:/books";
        } else {
            model = bookService.returnModelForBook(model, bookDao, book, true, false);
            return "books";
        }
    }
}
