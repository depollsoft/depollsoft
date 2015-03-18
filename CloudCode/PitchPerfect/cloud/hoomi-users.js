var _ = require('underscore');

var hoomiConfig = {
  applicationIds: []
};

var linkTable = "HoomiUserAssociations";

function requireMaster(request, response) {
  if (!request.master) {
    response.error();
  } else {
    response.success();
  }
}

// Secure the table, requiring the master key to ever write to it.
Parse.Cloud.beforeSave(linkTable, requireMaster);
Parse.Cloud.beforeDelete(linkTable, requireMaster);

var restrictedACL = new Parse.ACL();

function ensureSchema() {
  // Quick hack to ensure that the association table exists with the proper schema
  var object = new Parse.Object(linkTable);
  object.set('hoomiTokenInfo', {
    user_id: 'example'
  });
  var fakeUser = new Parse.User();
  fakeUser.id = "fake";
  object.set('parseUser', fakeUser);
  object.setACL(restrictedACL);
  return object.save(null, {useMasterKey: true}).then(function () {
    return object.destroy({useMasterKey: true});
  });
}

function getHoomiTokenInfo(token) {
  var url = 'https://api.hoomi.co/1/token/' + encodeURIComponent(token);
  console.log("Checking for token at: " + url);
  return Parse.Cloud.httpRequest({
    url: url,
    method: 'GET'
  }).then(function (result) {
    if (result.status < 200 || result.status >= 400) {
      return Parse.Promise.error(result);
    }
    return result.data;
  });
}

function queryAssociationForHoomiUserId(id) {
  var query = new Parse.Query(linkTable).
    equalTo('hoomiTokenInfo.user_id', id);
  return query;
}

function queryAssociationsForParseUser(user) {
  var query = new Parse.Query(linkTable).
    equalTo('parseUser', user);
  return query;
}

function linkUser(user, tokenInfo) {
  // Check the application id for the tokenInfo
  if (!_.contains(hoomiConfig.applicationIds, tokenInfo.application_id)) {
    return Parse.Promise.error("Token is not for the correct hoomi application")
  }

  return queryAssociationForHoomiUserId(tokenInfo.user_id).first({useMasterKey: true}).
    then(function (existingAssociation) {
      // If this user is already linked to someone else, fail.
      if (existingAssociation) {
        if (existingAssociation.get("parseUser").id !== user.id) {
          return Parse.Promise.error("Hoomi account already linked to another user.");
        } else {
          // Just update the token info on the existing assocation
          existingAssociation.set("hoomiTokenInfo", tokenInfo);
          return existingAssociation.save(null, {useMasterKey: true});
        }
      }

      // Otherwise, create an association
      var association = new Parse.Object(linkTable);
      association.set("hoomiTokenInfo", tokenInfo);
      association.set("parseUser", user);
      association.setACL(restrictedACL);
      return association.save(null, {useMasterKey: true});
    });
}

Parse.Cloud.define("HoomiLinkUser", function (request, response) {
  ensureSchema().then(function () {
    return getHoomiTokenInfo(request.params.hoomiToken);
  }).then(function (info) {
    return linkUser(request.user, info);
  }).then(function () {
    return null;
  }).then(response.success, response.error);
});

Parse.Cloud.define("HoomiUnlinkUser", function (request, response) {
  ensureSchema().then(function () {
    return  queryAssociationForHoomiUserId(request.params.hoomiUserId).first({useMasterKey: true});
  }).then(function (assocation) {
    if (assocation) {
      return assocation.destroy({useMasterKey: true});
    }
  }).then(response.success, response.error);
});

Parse.Cloud.define("HoomiLinkedTokenInfo", function (request, response) {
  ensureSchema().then(function () {
    return queryAssociationsForParseUser(request.user).find({useMasterKey: true});
  }).then(function (results) {
    return _.map(results, function (assocation) {
      return association.get("hoomiTokenInfo");
    });
  }).then(response.success, response.error);
});

Parse.Cloud.define("HoomiSignUpOrLogInUser", function (request, response) {
  var tokenInfo;
  var authenticatedUser;
  var isNew = false;
  ensureSchema().then(function () {
    return  getHoomiTokenInfo(request.params.hoomiToken);
  }).then(function (info) {
    tokenInfo = info;
    // Check the application id for the tokenInfo
    if (!_.contains(hoomiConfig.applicationIds, tokenInfo.application_id)) {
      return Parse.Promise.error("Token is not for the correct hoomi application")
    }
    // Look for an existing user
    return queryAssociationForHoomiUserId(info.user_id).
      first({useMasterKey: true});
  }).then(function (existingAssociation) {
    // Get the ParseUser (either new or existing)
    if (existingAssociation) {
      return existingAssociation.get("parseUser").fetch({useMasterKey: true});
    }
    isNew = true;
    var newUser;
    // Create a new ParseUser and link it
    return Parse.User.signUp(generateString(25), generateString(25), null, {useMasterKey: true});
  }).then(function (user) {
    // Link the user (this will update token info if this is a login)
    authenticatedUser = user;
    return linkUser(user, tokenInfo);
  }).then(function () {
    return {
      token: authenticatedUser.getSessionToken(),
      isNew: isNew
    };
  }).then(response.success, response.error);
});

function generateString(length) {
  var characters = [];
  var possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

  for(var i = 0; i < length; i++) {
      characters.push(possible.charAt(Math.floor(Math.random() * possible.length)));
  }

  return characters.join('');
}

exports.config = hoomiConfig;