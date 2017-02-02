var LoginPage = require('../pageObjects/login.po');  
var LandingPage = require('../pageObjects/landing.po');  
var HomePage = require('../pageObjects/home/home.po');  
var utils = require('../utils.js')

describe('Logins', function() {
  
  var loginPage = new LoginPage();
  var landingPage = new LandingPage();
  var homePage = new HomePage();
  
  beforeAll(function() {
    browser.driver.manage().window().maximize();
  });

  it('should not be able to login using the a wrong username & password', function() {  
    loginPage.navigate();  
    loginPage.login('fakeUsername', 'fakePassword')
    expect(browser.getCurrentUrl()).toContain("/login");
  });

  it('should be able to login and logout on landing page', function() {  
    loginPage.navigate();
    loginPage.login()
    expect(browser.getCurrentUrl()).toContain("/landing");
    expect(landingPage.getWelcomeMsg()).toContain('Welcome back');
    landingPage.logout();
  });
  
  it('should be able to login and then logout on home page', function() { 
    browser.refresh();
    loginPage.navigate();
    loginPage.login() 
    landingPage.getStarted();
    expect(browser.getCurrentUrl()).toContain("/home");
    homePage.logout();
  });

});