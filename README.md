# SuggestedProductExtractor needs this dependency:

https://mvnrepository.com/artifact/org.json/json/20140107


# Problem Statement:
Ecommerce was built to save time and conveninet shopping experience. But, there are still some issues finding related products on ecommerce websites.
To find a product you need to browse the description of the product and look at the reviews and ratings. This process takes time.

# Solution:
Now-a-days, we are using AI in our day to day life. i.e, ChatGPT etc. We can make use of AI to help us in the shopping experience.
I have built an application which can help us going through Walmart's catalog which shows 10 suggested items. 
And, with the help of AI, we can get the best item from it based on overall ratings, number of reviews and sentiments in those reviews.

# Demo:
Run SuggestedProductExtractorWithImage.java file.

# Features of the Application:
It can search through provided keyword and give us 10 suggested items and best item based on overall ratings, number of reviews and sentiments in those reviews.
The data is real-time.
The Appliction is making use of two APIs with a single click. (SerpAPI to get all the items best matching the given keyword, Google Gemini AI API to do the sentiment analysis of reviews and rating and providing us the best item.
It shows us the rating, number of reviews and price for each products it lists.
It provides sentiment analysis for the best product it provides.

# Future Enhancement and Usages:
Just like Walmart, this can be built for any other ecommerce website. Potentially we can analyze best product amnong all the websites.
We can give more data about suggested products and best product to make the decision easier.
We can provide user an ability to provide promt support to find the best product.
We can optimize the performance to narrow down results we are getting from SerpAPI.
With the use of AI, we can compare price and features of various products.
