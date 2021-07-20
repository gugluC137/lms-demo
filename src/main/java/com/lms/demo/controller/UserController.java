package com.lms.demo.controller;

import com.lms.demo.dao.model.User;
import com.lms.demo.dto.user.AddUserDto;
import com.lms.demo.dto.user.AddUserResponse;
import com.lms.demo.dto.user.BookBorrowDto;
import com.lms.demo.dto.user.BookBorrowResponse;
import com.lms.demo.error.*;
import com.lms.demo.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/user/add")
    public AddUserResponse addUser(@RequestBody AddUserDto addUserDto) throws IllegalPropertyValueException, DuplicateEntityException{

        //null check for name
        if(addUserDto.getName() == null) {
            throw new IllegalPropertyValueException(ErrorResponseMessages.nullNameValueForUser);
        }
        //null check for contact number
        if(addUserDto.getContactNumber() == null) {
            throw new IllegalPropertyValueException(ErrorResponseMessages.nullContactValueForUser);
        }

        return userService.saveUser(addUserDto);
    }

    @PostMapping("/user/borrow")
    public BookBorrowResponse borrowBook(@RequestBody BookBorrowDto bookBorrowDto) throws IllegalPropertyValueException, CopiesNotAvailableException, EntityNotFoundException {
        // null check for library id
        if(bookBorrowDto.getId() == null) {
            throw new IllegalPropertyValueException(ErrorResponseMessages.nullIdValueForBorrow);
        }

        //null check for isbn code
        if(bookBorrowDto.getIsbnCode() == null) {
            throw new IllegalPropertyValueException(ErrorResponseMessages.nullIsbnValueForBorrow);
        }

        return userService.saveBorrowBook(bookBorrowDto);
    }
}
