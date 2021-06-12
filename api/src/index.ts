import express from 'express';
import analyticsRouter from './analytics';

const app = express();
const port = process.env.PORT || 8080;

app.get('/', (req, res) => {
    res.send('Hello world!');
});

app.use('/analytics', analyticsRouter);

app.listen(port, () => {
    console.log(`Example app listening at http://localhost:${port}`);
});