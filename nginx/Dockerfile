FROM nginx:alpine

RUN rm /etc/nginx/nginx.conf

COPY /nginx/server.conf /etc/nginx/nginx.conf

EXPOSE 8081

CMD ["nginx", "-g", "daemon off;"]