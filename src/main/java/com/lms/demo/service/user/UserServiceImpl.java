package com.lms.demo.service.user;

import com.lms.demo.dao.model.BookItem;
import com.lms.demo.dao.model.BorrowDetails;
import com.lms.demo.dao.model.User;
import com.lms.demo.dao.repository.UserRepository;
import com.lms.demo.dto.mapper.BorrowDetailsMapper;
import com.lms.demo.dto.mapper.UserMapper;
import com.lms.demo.dto.user.*;
import com.lms.demo.error.*;
import com.lms.demo.service.book.BookItemService;
import com.lms.demo.service.book.BookService;
import com.lms.demo.service.borrow.BorrowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class UserServiceImpl implements UserService{

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BookService bookService;
    @Autowired
    private BookItemService bookItemService;
    @Autowired
    private BorrowService borrowService;

    @Override
    public AddUserResponse saveUser(AddUserDto addUserDto) throws IllegalPropertyValueException, DuplicateEntityException {
        UserMapper userMapper = new UserMapper();

        //duplicate name check
        if(userRepository.findByName(addUserDto.getName()) != null) {
            throw new DuplicateEntityException(ErrorResponseMessages.duplicateNameForUser);
        }
        //name validation
        if(addUserDto.getName().length() < 3) {
            throw new IllegalPropertyValueException(ErrorResponseMessages.illegalNameValueForUser);
        }

        //duplicate contact number check
        if(userRepository.findByContactNumber(addUserDto.getContactNumber()) != null) {
            throw new DuplicateEntityException(ErrorResponseMessages.duplicateContactForUser);
        }
        //contact number validation
        if(addUserDto.getContactNumber().length() != 10) {
            throw new IllegalPropertyValueException(ErrorResponseMessages.illegalContactValueForUser);
        }

        User user = userMapper.fromAddUser(addUserDto);

        AddUserResponse response = new AddUserResponse(userRepository.save(user));
        response.setMessage("user successfully created");
        return response;
    }

    public User getUserById(Long id) throws EntityNotFoundException {
        Optional<User> user = userRepository.findById(id);

        log.info("User found by id: {}", user);

        if(user.isPresent()) {
            return user.get();
        } else {
            log.warn("User not found!!!");
            throw new EntityNotFoundException(ErrorResponseMessages.userNotFound);
        }
    }

    @Override
    public BookBorrowResponse saveBorrowBook(BookBorrowDto bookBorrowDto) throws EntityNotFoundException, CopiesNotAvailableException {

        User user = getUserById(bookBorrowDto.getId());
        //book check
        if(!bookService.fetchBookById(bookBorrowDto.getIsbnCode()).isPresent()) {
            throw new EntityNotFoundException(ErrorResponseMessages.bookNotFound);
        }

        //book availability
        List<BookItem> copies = bookItemService.fetchByIsbnAndAvailability(bookBorrowDto.getIsbnCode(), true);
        if(copies.size() == 0) {
            throw new CopiesNotAvailableException(ErrorResponseMessages.copiesNotAvailable);
        }

        BookItem bookItem = copies.get(0);
        bookItemService.updateAvailable(bookItem.getBarcode(), false);

        BorrowDetailsMapper borrowDetailsMapper = new BorrowDetailsMapper();
        BorrowDetails borrowDetails = borrowDetailsMapper.fromBookBorrowDto(bookBorrowDto, user, bookItem);

        return new BookBorrowResponse(borrowService.saveBorrow(borrowDetails));
    }

    @Override
    public LibraryCardResponse fetchLibraryCard(GetLibraryCardDto getLibraryCardDto) throws EntityNotFoundException {

        //user exists check
        User user = getUserById(getLibraryCardDto.getId());

        // refresh fines
        borrowService.updateFine();

        //get active borrows for a user
        List<BorrowDetails> borrowDetailsList = borrowService.fetchActiveBorrowsByUserId(getLibraryCardDto.getId());

        return new LibraryCardResponse(user, borrowDetailsList);
    }

    @Override
    public ReturnBookResponse returnBook(
            final ReturnBookDto returnBookDto
    ) throws EntityNotFoundException, InvalidEntityException, BookAlreadyReturnedException {

        BorrowDetails borrowDetails = borrowService.fetchByIssueId(returnBookDto.getIssueId());

        //user doesn't exist
        if(!userRepository.existsById(returnBookDto.getLibraryId())) {
            throw new EntityNotFoundException(ErrorResponseMessages.userNotFound);
        }

        //invalid library id check
        if(!borrowDetails.getUser().getId().equals(returnBookDto.getLibraryId())) {
            throw new InvalidEntityException(ErrorResponseMessages.invalidLibraryIdForReturn);
        }
        //invalid barcode check
        if(!borrowDetails.getBookItem().getBarcode().equals(returnBookDto.getBarcode())) {
            throw new InvalidEntityException(ErrorResponseMessages.invalidBarcodeForReturn);
        }

        //check if book is already returned
        if(borrowDetails.getReturnDate() != null) {
            throw new BookAlreadyReturnedException(ErrorResponseMessages.bookAlreadyReturnedForReturn);
        }

        returnBookTransaction(borrowDetails, returnBookDto);

        return new ReturnBookResponse(borrowService.fetchByIssueId(borrowDetails.getId()));
    }

    @Transactional
    public void returnBookTransaction(
            BorrowDetails borrowDetails,
            ReturnBookDto returnBookDto) {
        borrowService.updateReturnDate(borrowDetails);
        borrowService.updateFine(borrowDetails);
        bookItemService.updateAvailable(returnBookDto.getBarcode(), true);
    }
}
