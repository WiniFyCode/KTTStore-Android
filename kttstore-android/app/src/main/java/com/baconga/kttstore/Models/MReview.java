package com.baconga.kttstore.Models;

public class MReview {
    public String reviewID;
    public String userID;
    public String productID;
    public int rating;
    public String comment;

    public MReview(String reviewID, String userID, String productID, int rating, String comment) {
        this.reviewID = reviewID;
        this.userID = userID;
        this.productID = productID;
        this.rating = rating;
        this.comment = comment;
    }

    public String getReviewID() {
        return reviewID;
    }

    public void setReviewID(String reviewID) {
        this.reviewID = reviewID;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getProductID() {
        return productID;
    }

    public void setProductID(String productID) {
        this.productID = productID;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
