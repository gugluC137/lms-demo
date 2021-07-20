package com.lms.demo.service.book;

import com.lms.demo.dao.model.BookItem;
import com.lms.demo.dao.repository.BookItemRepository;
import com.lms.demo.dao.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookItemServiceImpl implements BookItemService{

    @Autowired
    private BookItemRepository bookItemRepository;
    @Autowired
    private BookRepository bookRepository;

    @Override
    public BookItem saveBookItem(BookItem bookItem) {
        return bookItemRepository.save(bookItem);
    }

    @Override
    public List<BookItem> fetchByIsbnAndAvailability(Long isbn, Boolean availability) {
        return bookItemRepository.findByBookAndAvailable(bookRepository.getById(isbn), availability);
    }

    @Override
    public BookItem updateAvailable(Long barcode) {
        return bookItemRepository.updateAvailable(barcode);
    }
}