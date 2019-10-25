import { AppBar, Box, createMuiTheme, IconButton, Link, Paper, Toolbar, Typography } from '@material-ui/core';
import Button from '@material-ui/core/Button';
import { green } from '@material-ui/core/colors';
import CssBaseline from '@material-ui/core/CssBaseline';
import MenuIcon from '@material-ui/icons/Menu';
import { ThemeProvider } from '@material-ui/styles';
import React from 'react';
import { BrowserRouter as Router, Link as RouterLink, Route, RouteComponentProps } from 'react-router-dom';
import './App.css';

const App: React.FC = () => {
  const theme = createMuiTheme({
    palette: {
      primary: {
        main: green[500],
        contrastText: "#fff"
      }
    },
    spacing: 8
  });
  return (
    <React.Fragment>
      <Router>
        <ThemeProvider theme={theme}>
          <CssBaseline />
          <AppBar position="static">
            <Toolbar>
              <IconButton edge="start" color="inherit">
                <MenuIcon />
              </IconButton>
              <Typography variant="h6">
                qHarmony
              </Typography>
            </Toolbar>
          </AppBar>
          <Box p={1}>
            <Link component={RouterLink} to="/profiles/me">My Profile</Link><br/>
            <Link component={RouterLink} to="/search">Search</Link><br/>
            <Link component={RouterLink} to="/messages">Messages</Link><br/>
            <Link component={RouterLink} to="/">Home</Link><br/>
            <Route path="/" exact component={empty} />
            <Route path="/:text" component={stuff} />
          </Box>
        </ThemeProvider>
      </Router>
    </React.Fragment>
  );
};

function empty() {
  return <div>
    <Paper>Hi</Paper>
    <Button variant="contained" color="primary">Hello world!</Button>
  </div>
}

function stuff(props: RouteComponentProps<{ text: string }>) {
  return <div>{props.match.params.text}</div>;
}

export default App;
