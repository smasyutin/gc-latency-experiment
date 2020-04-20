FROM node:13
COPY main.js .
ENV NODE_OPTIONS='--max-old-space-size=1024'
ENTRYPOINT ["node", "./main"]
CMD ["1000000", "200000"]