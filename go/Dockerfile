FROM golang:1.14
COPY main.go .
RUN go build main.go
ENTRYPOINT ["./main"]
CMD ["1000000", "200000"]
